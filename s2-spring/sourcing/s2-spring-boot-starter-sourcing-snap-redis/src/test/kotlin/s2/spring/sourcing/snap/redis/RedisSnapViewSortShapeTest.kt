package s2.spring.sourcing.snap.redis

import kotlin.reflect.KClass
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Sorting by an attribute that is not a top-level text: nested, renamed, an array member, a tag. */
class RedisSnapViewSortShapeTest {

    data class Address(val city: String)

    data class ShapeProbeSnap(
        val id: String,
        val address: Address? = null,
        val aliases: List<String> = emptyList(),
        val status: String = "",
    )

    private val view = RedisStack.view(
        model = ShapeProbeSnap::class,
        fields = listOf(
            RedisIndexField(ShapeProbeSnap::id.name, RedisFieldType.TAG),
            RedisIndexField("address.city", RedisFieldType.TEXT, name = "city"),
            RedisIndexField.array(ShapeProbeSnap::aliases.name, RedisFieldType.TEXT),
            RedisIndexField(ShapeProbeSnap::status.name, RedisFieldType.TAG),
        ),
        idOf = ShapeProbeSnap::id,
    )

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

    private suspend fun ids(sortBy: String, descending: Boolean = false) =
        view.search(query = null, pagination = null, sortBy = sortBy, descending = descending).items.map { it.id }

    @Test
    suspend fun `a renamed text on a nested path sorts in collation order`() {
        listOf("z" to "Zèbre", "c" to "COFOR", "e" to "Érable", "l" to "Client").forEach { (id, city) ->
            val _ = view.save(ShapeProbeSnap(id = id, address = Address(city)))
        }
        val _ = view.save(ShapeProbeSnap(id = "n"))

        assertThat(ids("city")).containsExactly("l", "c", "e", "z", "n")
        assertThat(ids("city", descending = true)).containsExactly("n", "z", "e", "c", "l")
    }

    // Known gap of Redis Stack 6.2, pinned per stack: a text array of two values or more leaves the whole document
    // out of the index, without an error.
    @Test
    suspend fun `known gap - a text array keeps the store's own sort, and 6_2 indexes only single values`() {
        val _ = view.save(ShapeProbeSnap(id = "1", aliases = listOf("beta", "Alpha")))
        val _ = view.save(ShapeProbeSnap(id = "2", aliases = listOf("alpha")))

        if (RedisStack.isTextArrayIndexed) {
            assertThat(ids("aliases")).containsExactly("1", "2")
        } else {
            assertThat(ids("aliases")).containsExactly("2")
        }
    }

    @Test
    suspend fun `a tag, not sortable, still sorts in the store's byte order`() {
        listOf("b" to "open", "a" to "Closed", "c" to "archived").forEach { (id, status) ->
            val _ = view.save(ShapeProbeSnap(id = id, status = status))
        }

        assertThat(ids("status")).containsExactly("a", "c", "b")
    }

    @Test
    suspend fun `an attribute whose name holds a quote starts, sorted as the store sorts it`() {
        val quoted = RedisStack.view(
            model = ShapeProbeSnap::class,
            fields = listOf(
                RedisIndexField(ShapeProbeSnap::id.name, RedisFieldType.TAG),
                RedisIndexField(ShapeProbeSnap::status.name, RedisFieldType.TEXT, name = "status\"label"),
            ),
            idOf = ShapeProbeSnap::id,
        )

        quoted.initIndex()
        val _ = quoted.save(ShapeProbeSnap(id = "q", status = "open"))

        val found = quoted.search(query = null, pagination = null, sortBy = null).items.map { it.id }

        assertThat(found).containsExactly("q")
    }

    @Test
    suspend fun `a model reading the stored document back saves it again with fresh keys`() {
        @Suppress("UNCHECKED_CAST")
        val documents = RedisStack.view(
            model = Map::class as KClass<Map<String, Any?>>,
            fields = listOf(
                RedisIndexField("id", RedisFieldType.TAG),
                RedisIndexField("name", RedisFieldType.TEXT),
            ),
            idOf = { checkNotNull(it["id"]) as String },
        )
        documents.initIndex()
        val _ = documents.save(mapOf("id" to "m", "name" to "Alpha"))
        val read = checkNotNull(documents.get("m"))
        assertThat(read).containsKey(SortKeys.DOCUMENT_FIELD)

        val _ = documents.save(read + ("name" to "Zulu"))

        val saved = checkNotNull(documents.get("m"))
        val _ = documents.deleteAll()
        documents.dropIndex()
        assertThat(saved[SortKeys.DOCUMENT_FIELD]).isEqualTo(mapOf("name" to SortKeys.key(text = "Zulu", id = "m")))
    }
}
