package s2.spring.sourcing.snap.redis

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

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
                "$.labels[*] AS labels TAG",
        )
        assertThat(definition.createArgs("Probe").toCommandString()).startsWith("Probe ON JSON")
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
