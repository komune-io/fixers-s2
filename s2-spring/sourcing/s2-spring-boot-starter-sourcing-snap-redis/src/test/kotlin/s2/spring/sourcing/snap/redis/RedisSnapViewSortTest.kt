package s2.spring.sourcing.snap.redis

import f2.dsl.cqrs.page.OffsetPagination
import io.lettuce.core.api.reactive.RedisReactiveCommands
import io.lettuce.core.json.JsonPath
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.output.StatusOutput
import io.lettuce.core.output.ValueOutput
import io.lettuce.core.protocol.CommandArgs
import java.util.UUID
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Sorting by a text field follows the collation order whatever the Redis Stack, not the order of its bytes. */
class RedisSnapViewSortTest {

    data class SortProbeSnap(val id: String, val name: String? = null, val rank: Int? = null)

    private val fields = listOf(
        RedisIndexField(SortProbeSnap::id.name, RedisFieldType.TAG),
        RedisIndexField(SortProbeSnap::name.name, RedisFieldType.TEXT),
        RedisIndexField(SortProbeSnap::rank.name, RedisFieldType.NUMERIC),
    )

    private val view = RedisStack.view(model = SortProbeSnap::class, fields = fields, idOf = SortProbeSnap::id)

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
    suspend fun `a snapshot without the text sorts last, first when descending, on every page`() {
        val ids = store("Bravo", null, "alpha", null)
        val missing = listOf(ids[1], ids[3]).sorted()

        val pages = listOf(0, 3).flatMap { offset ->
            view.search(
                query = null,
                pagination = OffsetPagination(offset = offset, limit = 3),
                sortBy = "name",
                descending = false,
            ).items.map { it.id }
        }

        assertThat(pages).containsExactlyElementsOf(listOf(ids[2], ids[0]) + missing)
        assertThat(sorted(descending = true).map { it.id })
            .containsExactlyElementsOf(missing.reversed() + listOf(ids[0], ids[2]))
    }

    @Test
    suspend fun `descending pages follow one another`() {
        val _ = store("Zèbre", "COFOR", "Érable", "Client", "Charpentier")

        val pages = listOf(0, 2, 4).flatMap { offset ->
            sorted(descending = true, pagination = OffsetPagination(offset = offset, limit = 2)).map { it.name }
        }

        assertThat(pages).containsExactly("Zèbre", "Érable", "COFOR", "Client", "Charpentier")
    }

    @Test
    suspend fun `a text query is sorted too`() {
        val _ = store("Érable rouge", "érable blanc", "Chêne", "Érable argenté")

        val names = view.search(query = "érable", pagination = null, sortBy = "name").items.map { it.name }

        assertThat(names).containsExactly("Érable argenté", "érable blanc", "Érable rouge")
    }

    @Test
    suspend fun `equal names sort by the bytes of their ids, whatever their length or case`() {
        val ids = listOf("b", "B", "aa", "a", "A-1", "ab")
        ids.forEach { id -> val _ = view.save(SortProbeSnap(id = id, name = "Même")) }

        assertThat(sorted().map { it.id }).containsExactly("A-1", "B", "a", "aa", "ab", "b")
    }

    @Test
    suspend fun `texts equal over their sorted length sort by id`() {
        val head = "x".repeat(SortKeys.SORTED_LENGTH)
        val _ = view.save(SortProbeSnap(id = "b", name = head + "a"))
        val _ = view.save(SortProbeSnap(id = "a", name = head + "z"))
        val _ = view.save(SortProbeSnap(id = "c", name = "x".repeat(SortKeys.SORTED_LENGTH - 1)))

        assertThat(sorted().map { it.id }).containsExactly("c", "a", "b")
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
    suspend fun `snapshots stored under the previous index get their sort keys at the next start, without a reload`() {
        view.dropIndex()
        val created = commands.dispatch<String>(
            SearchCommand.FT_CREATE,
            StatusOutput(StringCodec.UTF8),
            PREVIOUS_DEFINITION.split(" ").fold(CommandArgs(StringCodec.UTF8).add(view.indexName)) { args, argument ->
                args.add(argument)
            },
        ).awaitFirst()
        assertThat(created).isEqualTo("OK")
        val ids = listOf("Zèbre", "COFOR", "Érable", "Client", "Charpentier").map { name -> storeUnkeyed(name) }
        assertThat(ids).hasSize(5)

        view.initIndex()

        assertThat(sortedNames()).containsExactly("Charpentier", "Client", "COFOR", "Érable", "Zèbre")
    }

    @Test
    suspend fun `a snapshot written without its sort key gets it at the next start`() {
        val _ = store("Bravo", "Charlie")
        val _ = storeUnkeyed("Alpha")

        view.initIndex()
        view.initIndex()

        assertThat(sortedNames()).containsExactly("Alpha", "Bravo", "Charlie")
    }

    @Test
    suspend fun `a sort key is not written over a snapshot saved since it was read`() {
        val id = storeUnkeyed("Alpha")
        val read = checkNotNull(rawDocument(id))
        val _ = view.save(SortProbeSnap(id = id, name = "Zulu"))
        val backfill = SortKeyBackfill(
            connection = RedisStack.connection,
            indexName = view.indexName,
            keys = SnapshotKeys(
                connection = RedisStack.connection,
                prefix = view.indexName,
                model = SortProbeSnap::class,
                objectMapper = RedisSnapView.defaultObjectMapper(),
            ),
            sortKeys = IndexDefinition.from(view.indexName, fields, RedisStack.settings).sortKeys,
            objectMapper = RedisSnapView.defaultObjectMapper(),
        )

        val isWritten = backfill.writeIfUnchanged(key = view.key(id), read = read, sortKeys = """{"name":"stale"}""")

        assertThat(isWritten).isFalse()
        assertThat(sortKeyOf(id)).isEqualTo(SortKeys.key(text = "Zulu", id = id))
        assertThat(backfill.key(view.key(id))).isEqualTo(SortKeyBackfill.Outcome.CURRENT)
    }

    @Test
    fun `saves running while the start keys the stored snapshots keep the keys of what they saved`(): Unit =
        runBlocking {
            val ids = (1..CONCURRENT).map { storeUnkeyed("before $it") }

            coroutineScope {
                launch { view.initIndex() }
                ids.forEachIndexed { i, id -> launch { val _ = view.save(SortProbeSnap(id = id, name = "after $i")) } }
            }

            ids.forEach { id ->
                val name = checkNotNull(view.get(id)?.name)
                assertThat(sortKeyOf(id)).describedAs(id).isEqualTo(SortKeys.key(text = name, id = id))
            }
        }

    private suspend fun storeUnkeyed(name: String): String {
        val id = "sort-probe-${UUID.randomUUID()}"
        val answer = commands.jsonSet(
            view.key(id),
            JsonPath.of("$"),
            """{"id":"$id","name":"$name","type":"${view.indexName}"}""",
        ).awaitSingle()
        assertThat(answer).isEqualTo("OK")
        return id
    }

    private suspend fun rawDocument(id: String): String? = commands.dispatch<String>(
        SearchCommand.cmd("JSON.GET"),
        ValueOutput(StringCodec.UTF8),
        CommandArgs(StringCodec.UTF8).addKey(view.key(id)),
    ).awaitFirstOrNull()

    private suspend fun sortKeyOf(id: String): String =
        commands.jsonGet(view.key(id), JsonPath.of("$.__s2_sort.name")).awaitFirst().toString().trim('[', ']', '"')

    private companion object {
        const val KEPT_ALIAS = "SortProbeSnapKept"
        const val CONCURRENT = 50

        // The definition views wrote before sort keys: no key attribute, no marker.
        const val PREVIOUS_DEFINITION = "ON JSON PREFIX 1 SortProbeSnap- STOPWORDS 0 SCHEMA " +
            "$.id AS id TAG $.name AS name TEXT NOSTEM PHONETIC dm:fr SORTABLE $.rank AS rank NUMERIC SORTABLE"
    }
}
