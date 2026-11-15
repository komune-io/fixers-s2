package s2.spring.sourcing.snap.redis

import f2.dsl.cqrs.page.OffsetPagination
import io.lettuce.core.api.reactive.RedisReactiveCommands
import io.lettuce.core.json.JsonPath
import io.lettuce.core.search.arguments.SearchArgs
import io.lettuce.core.search.arguments.SortByArgs
import java.util.UUID
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Sorting by a text field follows the collation order whatever the Redis Stack, not the order of its bytes. */
class RedisSnapViewSortTest {

    data class SortProbeSnap(val id: String, val name: String? = null, val rank: Int? = null)

    private val view = RedisStack.view(
        model = SortProbeSnap::class,
        fields = listOf(
            RedisIndexField(SortProbeSnap::id.name, RedisFieldType.TAG),
            RedisIndexField(SortProbeSnap::name.name, RedisFieldType.TEXT),
            RedisIndexField(SortProbeSnap::rank.name, RedisFieldType.NUMERIC),
        ),
        idOf = SortProbeSnap::id,
    )

    private val commands = RedisStack.connection.commands(RedisReactiveCommands.factory())

    @BeforeEach
    fun prepare() {
        runBlocking {
            val _ = view.deleteAll()
            view.initIndex()
        }
    }

    @AfterEach
    fun tidy() {
        runBlocking {
            val _ = view.deleteAll()
            view.dropIndex()
        }
    }

    private suspend fun store(vararg names: String?): List<String> =
        names.map { name ->
            "sort-probe-${UUID.randomUUID()}".also { id -> val _ = view.save(SortProbeSnap(id = id, name = name)) }
        }

    private suspend fun sorted(descending: Boolean = false, pagination: OffsetPagination? = null) =
        view.search(query = null, pagination = pagination, sortBy = "name", descending = descending).items

    private suspend fun sortedNames(descending: Boolean = false) = sorted(descending).map { it.name }

    @Test
    suspend fun `a text sort ignores case and accents, both ways`() {
        val _ = store("Zèbre", "COFOR", "Érable", "Client", "Charpentier")
        val collated = listOf("Charpentier", "Client", "COFOR", "Érable", "Zèbre")

        assertThat(sortedNames()).containsExactlyElementsOf(collated)
        assertThat(sortedNames(descending = true)).containsExactlyElementsOf(collated.reversed())
    }

    @Test
    suspend fun `pages follow one another in collation order`() {
        val _ = store("Zèbre", "COFOR", "Érable", "Client", "Charpentier")

        val pages = listOf(0, 2, 4).map { offset ->
            view.search(
                query = null,
                pagination = OffsetPagination(offset = offset, limit = 2),
                sortBy = "name",
                descending = false,
            )
        }

        assertThat(pages.flatMap { page -> page.items.map { it.name } })
            .containsExactly("Charpentier", "Client", "COFOR", "Érable", "Zèbre")
        assertThat(pages.map { it.total }).containsOnly(5)
    }

    @Test
    suspend fun `names equal but for accents or case take a fixed order, accents then case`() {
        val _ = store("côté", "Éclair", "coté", "eclair", "côte", "Eclair", "cote", "ecluse")

        assertThat(sortedNames())
            .containsExactly("cote", "coté", "côte", "côté", "eclair", "Eclair", "Éclair", "ecluse")
    }

    @Test
    suspend fun `digits and words sort character by character, an empty name first`() {
        val _ = store("Item 2", "item 10", "9", "10", "", "Item")

        assertThat(sortedNames()).containsExactly("", "10", "9", "Item", "item 10", "Item 2")
    }

    @Test
    suspend fun `equal names take one order on every page, reversed when descending`() {
        val ids = store(*Array(6) { "Même" }, "Autre")
        val byId = ids.dropLast(1).sorted()

        val pages = listOf(0, 2, 4, 6).flatMap { offset ->
            view.search(null, OffsetPagination(offset = offset, limit = 2), "name").items.map { it.id }
        }

        assertThat(pages).containsExactlyElementsOf(listOf(ids.last()) + byId)
        assertThat(sorted(descending = true).map { it.id }).containsExactlyElementsOf(byId.reversed() + ids.last())
    }

    @Test
    suspend fun `a snapshot without the field takes the place the store gives a missing value`() {
        val ids = store("Bravo", null, "alpha")

        listOf(false, true).forEach { descending ->
            val raw = rawSortedIds(descending)

            assertThat(sorted(descending).map { it.id }.indexOf(ids[1]))
                .describedAs("descending: $descending, store order $raw")
                .isEqualTo(raw.indexOf(ids[1]))
        }
        assertThat(sortedNames().filterNotNull()).containsExactly("alpha", "Bravo")
    }

    @Test
    suspend fun `an index holding sort keys is read back as its own, and kept at the next start`() {
        val sync = RedisStack.connection.sync()
        sync.ftAliasadd(KEPT_ALIAS, view.indexName)
        try {
            view.initIndex()

            assertThat(view.index.info(KEPT_ALIAS)).isNotNull()
        } finally {
            sync.ftAliasdel(KEPT_ALIAS)
        }
    }

    @Test
    suspend fun `a numeric sort is the store's own`() {
        listOf(10, 9, 100).forEach { rank ->
            val _ = view.save(SortProbeSnap(id = "sort-probe-${UUID.randomUUID()}", rank = rank))
        }

        val ranks = view.search(query = null, pagination = null, sortBy = "rank", descending = false)
            .items
            .map { it.rank }

        assertThat(ranks).containsExactly(9, 10, 100)
    }

    @Test
    suspend fun `the sort key is neither in the snapshot read nor found by a search`() {
        val id = store("Érable").single()
        val stored = commands.jsonGet(view.key(id), JsonPath.of("$.__s2_sort.name")).awaitFirst().toString()
        val collationKey = stored.trim('[', ']', '"').substringBefore(' ')

        assertThat(collationKey).matches("[0-9a-f]{8,}")
        assertThat(view.get(id)).isEqualTo(SortProbeSnap(id = id, name = "Érable"))
        assertThat(view.search(collationKey, null, null).items).isEmpty()
        assertThat(view.search(collationKey.take(4).addWildcard(), null, null).items).isEmpty()
        assertThat(view.search("érable", null, null).items.map { it.id }).containsExactly(id)
    }

    @Test
    suspend fun `a snapshot written without its sort key sorts as a missing value until it is saved again`() {
        val ids = store("Bravo", "Charlie")
        val legacy = "sort-probe-${UUID.randomUUID()}"
        val answer = commands.jsonSet(
            view.key(legacy),
            JsonPath.of("$"),
            """{"id":"$legacy","name":"Alpha","type":"${view.indexName}"}""",
        ).awaitSingle()
        assertThat(answer).isEqualTo("OK")
        val _ = view.index.awaitIndexed()

        assertThat(sorted().map { it.id }.indexOf(legacy)).isIn(0, 2)
        assertThat(sorted().map { it.id }.filter { it in ids }).containsExactlyElementsOf(ids)

        val _ = view.save(checkNotNull(view.get(legacy)))

        assertThat(sortedNames()).containsExactly("Alpha", "Bravo", "Charlie")
    }

    private suspend fun rawSortedIds(descending: Boolean): List<String> {
        val order = SortByArgs.builder().attribute("name")
        val args = SearchArgs.builder<String>()
            .sortBy((if (descending) order.descending() else order).build())
            .limit(0, SEARCH_WINDOW.toLong())
            .build()
        return commands.ftSearch(view.indexName, "*", args).awaitSingle()
            .results
            .map { it.id.removePrefix("${view.indexName}-") }
    }

    private companion object {
        const val KEPT_ALIAS = "SortProbeSnapKept"
    }
}
