package s2.spring.sourcing.snap.redis

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ObjectNode

class IndexDefinitionTest {

    private val fields = listOf(
        RedisIndexField("id", RedisFieldType.TAG),
        RedisIndexField("name", RedisFieldType.TEXT),
        RedisIndexField("rank", RedisFieldType.NUMERIC),
        RedisIndexField.array("labels", RedisFieldType.TAG),
    )

    @Test
    fun `an index reads its model's keys, keeps every word unstemmed and sets each type's flags`() {
        val definition = IndexDefinition.from("Probe", fields, IndexSettings(PhoneticMatcher.FRENCH))

        assertThat(definition.fingerprint).isEqualTo(
            "ON JSON PREFIX 1 Probe- STOPWORDS 0 SCHEMA " +
                "$.id AS id TAG " +
                "$.name AS name TEXT NOSTEM PHONETIC dm:fr SORTABLE " +
                "$.rank AS rank NUMERIC SORTABLE " +
                "$.labels[*] AS labels TAG " +
                "$.__s2_sort[\"name\"] AS __s2_sort_name TEXT NOINDEX SORTABLE UNF",
        )
        assertThat(definition.createArgs("Probe").toCommandString()).startsWith("Probe ON JSON")
    }

    @Test
    fun `a text field is sorted by its key, unless its path walks into an array`() {
        val texts = listOf(
            RedisIndexField("id", RedisFieldType.TAG),
            RedisIndexField("address.city", RedisFieldType.TEXT, name = "city"),
            RedisIndexField.array("aliases", RedisFieldType.TEXT),
            RedisIndexField("rank", RedisFieldType.NUMERIC),
        )
        val sortKeys = IndexDefinition.from("Probe", texts, IndexSettings(null)).sortKeys

        assertThat(sortKeys.sortAttribute("city")).isEqualTo("__s2_sort_city")
        assertThat(sortKeys.sortAttribute("aliases")).isEqualTo("aliases")
        assertThat(sortKeys.sortAttribute("rank")).isEqualTo("rank")
        val document = JsonMapper.builder().build().readTree(
            """{"id":"a","address":{"city":"Érable"},"aliases":["x"],"rank":1}""",
        ) as ObjectNode
        sortKeys.write(document = document, id = "a")

        assertThat(document.get(SortKeys.DOCUMENT_FIELD).propertyNames()).containsExactly("city")
        assertThat(document.get(SortKeys.DOCUMENT_FIELD).get("city").stringValue()).endsWith(" a")
    }

    @Test
    fun `a sort key orders texts as the collator does, then by id`() {
        val words = listOf("Zèbre", "COFOR", "Érable", "Client", "Charpentier", "cote", "côté", "coté", "côte", "")
        val collated = listOf("", "Charpentier", "Client", "COFOR", "cote", "coté", "côte", "côté", "Érable", "Zèbre")

        assertThat(words.sortedBy { SortKeys.key(text = it, id = "same") }).containsExactlyElementsOf(collated)
        assertThat(listOf("b", "a", "ab").sortedBy { SortKeys.key(text = "même", id = it) })
            .containsExactly("a", "ab", "b")
        assertThat(SortKeys.key(text = "e\u0301", id = "x")).isEqualTo(SortKeys.key(text = "\u00e9", id = "x"))
    }

    @Test
    fun `a collation key is pinned, so that a JVM sorting otherwise fails the build`() {
        val golden = mapOf(
            "" to "00000000",
            "a" to "00530000000100000001",
            "A" to "00530000000100000002",
            "é" to "005800000001008900000001",
            "Érable" to "0058006500530054005f0058000000010089000100010001000100010000000200010001000100010001",
            "日本" to "800065e68000672d0000000100010001000100000001000100010001",
            "\uD83C\uDF32" to "8000d83ddf3300000001000100010000000100010001",
        )

        golden.forEach { (text, hex) -> assertThat(SortKeys.collationHex(text)).describedAs("«$text»").isEqualTo(hex) }
    }

    @Test
    fun `a missing or non-text value gets the key sorting after every text`() {
        val sortKeys = IndexDefinition.from("Probe", fields, IndexSettings(null)).sortKeys
        val document = JsonMapper.builder().build().readTree("""{"id":"a","name":12}""") as ObjectNode

        assertThat(sortKeys.keysOf(document = document, id = "a").get("name").stringValue()).isEqualTo("~ a")
        assertThat(SortKeys.missing("a")).isGreaterThan(SortKeys.key(text = "\uFFFF".repeat(3), id = "a"))
    }

    @Test
    fun `an attribute or a path that is not a plain dotted name keeps the store's sort`() {
        assertThat(SortKeys.sorted(attribute = "wood-species", path = "woodSpecies")).isNull()
        assertThat(SortKeys.sorted(attribute = "name\"]", path = "name")).isNull()
        assertThat(SortKeys.sorted(attribute = "aliases", path = "aliases[*]")).isNull()
        assertThat(SortKeys.sorted(attribute = "city", path = "address.city")?.pointer).isEqualTo("/address/city")
    }

    @Test
    fun `a field under the sort keys' own name is refused, and a quote in an attribute name gives no key`() {
        val reading = listOf(RedisIndexField("__s2_sort.name", RedisFieldType.TEXT, name = "sorted"))
        val quoted = listOf(RedisIndexField("city", RedisFieldType.TEXT, name = "city\"label"))

        assertThatThrownBy { val _ = IndexDefinition.from("Probe", reading, IndexSettings(null)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("__s2_sort")
        assertThat(IndexDefinition.from("Probe", quoted, IndexSettings(null)).fingerprint).doesNotContain("__s2_sort")
    }

    @Test
    fun `a snapshot holding the sort keys' own field is refused before it is written`() {
        val sortKeys = IndexDefinition.from("Probe", fields, IndexSettings(null)).sortKeys
        val document = JsonMapper.builder().build().readTree("""{"id":"a","name":"x","__s2_sort":1}""") as ObjectNode

        assertThatThrownBy { sortKeys.write(document = document, id = "a") }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("__s2_sort")
    }

    @Test
    fun `no language is sent, words being never stemmed`() {
        assertThat(IndexDefinition.from("Probe", fields, IndexSettings(null)).fingerprint).doesNotContain("LANGUAGE")
    }

    @Test
    fun `a change of phonetic matcher changes the definition`() {
        val french = IndexDefinition.from("Probe", fields, IndexSettings(PhoneticMatcher.FRENCH))
        val english = IndexDefinition.from("Probe", fields, IndexSettings(PhoneticMatcher.ENGLISH))
        val none = IndexDefinition.from("Probe", fields, IndexSettings(null))

        assertThat(setOf(french.fingerprint, english.fingerprint, none.fingerprint)).hasSize(3)
        // FT.INFO does not show the matcher: the marker attribute, which it shows, carries the difference.
        assertThat(setOf(french.marker, english.marker, none.marker)).hasSize(3)
        assertThat(none.fingerprint).doesNotContain("PHONETIC")
    }

    @Test
    fun `an index names each field once, and at least one`() {
        val twice = fields + RedisIndexField("name", RedisFieldType.TAG)

        assertThatThrownBy { val _ = IndexDefinition.from("Probe", twice, IndexSettings(null)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("names a field twice")
        assertThatThrownBy { val _ = IndexDefinition.from("Probe", emptyList(), IndexSettings(null)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("at least one field")
    }

    @Test
    fun `FT INFO is read in both protocols, and an index that reads otherwise is not this one`() {
        val definition = IndexDefinition.from(
            indexName = "Probe",
            fields = listOf(RedisIndexField("id", RedisFieldType.TAG)),
            settings = IndexSettings(null),
        )
        val resp2 = mapOf(
            "index_definition" to listOf("key_type", "JSON", "prefixes", listOf("Probe-"), "default_score", "1"),
            "attributes" to listOf(
                listOf("identifier", "$.id", "attribute", "id", "type", "TAG", "SEPARATOR", ","),
                listOf("identifier", MARKER_PATH, "attribute", definition.marker, "type", "TAG", "SEPARATOR", ","),
            ),
            "stopwords_list" to emptyList<Any?>(),
        )
        val resp3 = mapOf(
            "index_definition" to mapOf("key_type" to "JSON", "prefixes" to listOf("Probe-")),
            "attributes" to listOf(
                mapOf("identifier" to "$.id", "attribute" to "id", "type" to "TAG"),
                mapOf("identifier" to MARKER_PATH, "attribute" to definition.marker, "type" to "TAG"),
            ),
            "stopwords_list" to emptyList<Any?>(),
        )

        assertThat(definition.isShownBy(resp2)).isTrue()
        assertThat(definition.isShownBy(resp3)).isTrue()
        assertThat(definition.isShownBy(resp3 + ("stopwords_list" to listOf("a")))).isFalse()
        assertThat(definition.isShownBy(resp3 + ("attributes" to emptyList<Any?>()))).isFalse()
        val unmarked = listOf(mapOf("identifier" to "$.id", "attribute" to "id", "type" to "TAG"))
        assertThat(definition.isShownBy(resp3 + ("attributes" to unmarked))).isFalse()
        val french = mapOf("prefixes" to listOf("Probe-"), "default_language" to "french")
        val otherPrefix = mapOf("prefixes" to listOf("Probe"))

        assertThat(definition.isShownBy(resp3 + ("index_definition" to french))).isFalse()
        assertThat(definition.isShownBy(resp3 + ("index_definition" to otherPrefix))).isFalse()
    }

    @Test
    fun `an FT INFO answer that cannot be read is an error, not an absent index`() {
        val definition = IndexDefinition.from(
            indexName = "Probe",
            fields = listOf(RedisIndexField("id", RedisFieldType.TAG)),
            settings = IndexSettings(null),
        )

        assertThat(asFields(listOf("odd"))).isNull()
        val noDefinition = mapOf("attributes" to emptyList<Any?>())
        val badAttribute = mapOf("index_definition" to emptyMap<String, Any?>(), "attributes" to listOf(listOf("x")))

        assertThat(runCatching { definition.isShownBy(noDefinition) }.exceptionOrNull())
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("index_definition")
        assertThat(runCatching { definition.isShownBy(badAttribute) }.exceptionOrNull())
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("attribute unreadable")
    }

    private companion object {
        const val MARKER_PATH = "$.__s2_index_definition"
    }
}
