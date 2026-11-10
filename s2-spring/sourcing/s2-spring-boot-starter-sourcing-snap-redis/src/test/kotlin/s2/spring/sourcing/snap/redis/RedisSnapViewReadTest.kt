package s2.spring.sourcing.snap.redis

import f2.dsl.cqrs.exception.F2Exception
import f2.dsl.cqrs.page.OffsetPagination
import io.lettuce.core.json.DefaultJsonParser
import io.lettuce.core.search.SearchReply
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/**
 * How answers are read, without a store: `JSON.MGET key… $` answers per key an array holding the root
 * document or a nil, and a search hit may come without its document.
 */
class RedisSnapViewReadTest {

    data class Model(val id: String)

    private val parser = DefaultJsonParser()

    private fun hit(key: String, document: String? = null) = SearchReply.SearchResult(key).apply {
        if (document != null) addField("$", document.toByteArray(Charsets.UTF_8))
    }

    private fun reply(vararg hits: SearchReply.SearchResult<String>) = SearchReply<String>().apply {
        hits.forEach { addResult(it) }
    }

    private fun decode(hit: Pair<String, String>) = Model(hit.second.substringAfter(":\"").substringBefore("\""))

    private fun whole(total: Long, vararg hits: SearchReply.SearchResult<String>) =
        RedisSnapView.searchPage(
            indexName = "Model",
            count = total,
            read = reply(*hits).documents(),
            pagination = null,
            decode = ::decode,
        )

    @Test
    fun `a root-path array yields the document it holds`() {
        assertThat(parser.createJsonValue("""[{"id":"a"}]""").rootDocument()).isEqualTo("""{"id":"a"}""")
    }

    @Test
    fun `an object is taken as the document itself`() {
        assertThat(parser.createJsonValue("""{"id":"a"}""").rootDocument()).isEqualTo("""{"id":"a"}""")
    }

    @Test
    fun `a nil, an empty array or an array holding null is an absent document`() {
        assertThat(parser.loadJsonValue(null).rootDocument()).isNull()
        assertThat(parser.createJsonValue("[]").rootDocument()).isNull()
        assertThat(parser.createJsonValue("[null]").rootDocument()).isNull()
    }

    @Test
    fun `a hit without a document is reported by its key, the others read in order`() {
        val read = reply(
            hit(key = "Model-1", document = """{"id":"1"}"""),
            hit(key = "Model-2"),
            hit(key = "Model-3", document = """{"id":"3"}"""),
        ).documents()

        assertThat(read.documents).containsExactly("Model-1" to """{"id":"1"}""", "Model-3" to """{"id":"3"}""")
        assertThat(read.missing).containsExactly("Model-2")
    }

    @Test
    fun `a document is read as UTF-8 whatever the JVM default charset`() {
        val read = reply(hit(key = "Model-1", document = """{"id":"Égrève"}""")).documents()

        assertThat(read.documents).containsExactly("Model-1" to """{"id":"Égrève"}""")
    }

    @Test
    fun `a hit whose document is gone is left out of a whole read, the other rows answered`() {
        val page = whole(
            3,
            hit(key = "Model-1", document = """{"id":"1"}"""),
            hit(key = "Model-2"),
            hit(key = "Model-3", document = """{"id":"3"}"""),
        )

        assertThat(page.items).containsExactly(Model("1"), Model("3"))
        assertThat(page.total).isEqualTo(2)
    }

    @Test
    fun `a whole read answered fewer hits than its total is refused`() {
        assertThatThrownBy { val _ = whole(3, hit(key = "Model-1", document = """{"id":"1"}"""), hit(key = "Model-2")) }
            .isInstanceOfSatisfying(F2Exception::class.java) { refusal ->
                assertThat(refusal.error.refusal?.code).isEqualTo(SearchRefusal.SHORT_READ)
                assertThat(refusal.error.code).isEqualTo(409)
                assertThat(refusal.error.refusal?.parameters).containsExactlyInAnyOrderEntriesOf(
                    mapOf("index" to "Model", "total" to "2", "read" to "1"),
                )
            }
    }

    @Test
    fun `a whole read past the window is refused, missing documents or not`() {
        assertThatThrownBy { val _ = whole(SEARCH_WINDOW + 1L, hit(key = "Model-1")) }
            .isInstanceOfSatisfying(F2Exception::class.java) { refusal ->
                assertThat(refusal.error.refusal?.code).isEqualTo(SearchRefusal.TOO_MANY_ENTRIES)
                assertThat(refusal.error.code).isEqualTo(422)
                assertThat(refusal.error.refusal?.parameters).containsExactlyInAnyOrderEntriesOf(
                    mapOf("index" to "Model", "total" to "$SEARCH_WINDOW", "bound" to "${SEARCH_WINDOW - 1}"),
                )
            }
    }

    @Test
    fun `a page leaves a hit without a document out of its rows, its total as counted, and is never refused`() {
        val documents = reply(hit(key = "Model-1", document = """{"id":"1"}"""), hit(key = "Model-2")).documents()

        val page = RedisSnapView.searchPage(
            indexName = "Model",
            count = 3,
            read = documents,
            pagination = OffsetPagination(offset = 0, limit = 10),
            decode = ::decode,
        )

        assertThat(page.items).containsExactly(Model("1"))
        assertThat(page.total).isEqualTo(3)
    }
}
