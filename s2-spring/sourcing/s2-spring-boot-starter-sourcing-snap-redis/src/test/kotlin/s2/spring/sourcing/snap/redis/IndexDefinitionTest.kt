package s2.spring.sourcing.snap.redis

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class IndexDefinitionTest {

	private fun resp2(vararg attributes: List<Any?>) = mapOf(
		"index_definition" to listOf("key_type", "JSON", "prefixes", listOf("Probe-"), "default_language", "english"),
		"attributes" to attributes.toList(),
		"stopwords_list" to emptyList<Any?>(),
	)

	private val name = listOf(RedisIndexField("name", RedisFieldType.TEXT))

	@Test
	fun `a RESP2 attribute is read by position, even when a value repeats a key`() {
		val read = IndexDefinition.read(
			resp2(listOf("identifier", "$.type", "attribute", "type", "type", "TAG", "SEPARATOR", ",")),
		)

		assertThat(read?.attributes).containsExactly(IndexDefinition.Attribute("$.type", "type", "TAG", emptySet()))
		assertThat(read?.language).isEqualTo("english")
	}

	@Test
	fun `an index whose answer names no language is in RediSearch's default, English`() {
		val read = IndexDefinition.read(
			mapOf(
				"index_definition" to listOf("key_type", "JSON", "prefixes", listOf("Probe-")),
				"attributes" to emptyList<Any?>(),
			),
		)

		assertThat(read?.language).isEqualTo(IndexLanguage.ENGLISH.value)
	}

	@Test
	fun `an answer that cannot be read is no definition, not an empty one`() {
		assertThat(IndexDefinition.read(mapOf("index_definition" to listOf("default_language", "english")))).isNull()
		assertThat(IndexDefinition.read(mapOf("index_definition" to listOf("prefixes", "Probe-")))).isNull()
		assertThat(IndexDefinition.read(mapOf("attributes" to emptyList<Any?>()))).isNull()
		assertThat(IndexDefinition.read(resp2(listOf("attribute", "id", "identifier", "$.id")))).isNull()
	}

	@Test
	fun `the creation sends the flags the definition holds, and only those`() {
		val bare = IndexDefinition(
			listOf("Probe-"),
			"english",
			false,
			listOf(IndexDefinition.Attribute("$.n", "n", "TEXT", setOf("NOSTEM"))),
			phonetic = null,
		)

		val sent = bare.createArgs("Probe").toCommandString()

		assertThat(sent).contains("NOSTEM").doesNotContain("SORTABLE").doesNotContain("PHONETIC").contains("STOPWORDS 0")
	}

	@Test
	fun `the language and the phonetic matcher are the settings' own`() {
		val sent = IndexDefinition.of("Probe", name, IndexSettings(IndexLanguage.SPANISH, PhoneticMatcher.SPANISH))
			.createArgs("Probe").toCommandString()

		assertThat(sent).contains("LANGUAGE spanish").contains("PHONETIC dm:es")
	}

	@Test
	fun `no phonetic matcher sends none`() {
		val sent = IndexDefinition.of("Probe", name, IndexSettings(IndexLanguage.ENGLISH, null))
			.createArgs("Probe").toCommandString()

		assertThat(sent).contains("LANGUAGE english").doesNotContain("PHONETIC")
	}

	@Test
	fun `two definitions in different languages differ, so the index is made again`() {
		val english = IndexDefinition.of("Probe", name, IndexSettings(IndexLanguage.ENGLISH, null))
		val french = IndexDefinition.of("Probe", name, IndexSettings(IndexLanguage.FRENCH, null))

		assertThat(english).isNotEqualTo(french)
	}

	@Test
	fun `the phonetic matcher, which FT INFO does not show, is not compared`() {
		val with = IndexDefinition.of("Probe", name, IndexSettings(IndexLanguage.ENGLISH, PhoneticMatcher.ENGLISH))
		val without = IndexDefinition.of("Probe", name, IndexSettings(IndexLanguage.ENGLISH, null))

		assertThat(with).isEqualTo(without).hasSameHashCodeAs(without)
	}

	@Test
	fun `an index reads its model's keys only, the dash keeping a longer model name out`() {
		val definition = IndexDefinition.of("Probe", name, IndexSettings(IndexLanguage.ENGLISH, null))

		assertThat(definition.prefixes).containsExactly("Probe-")
		assertThat(definition.createArgs("Probe").toCommandString()).contains("PREFIX 1 Probe-")
	}

	@Test
	fun `an index reading another prefix differs, so it is made again`() {
		val wanted = IndexDefinition.of("Probe", name, IndexSettings(IndexLanguage.ENGLISH, null))

		assertThat(wanted.copy(prefixes = listOf("Probe"))).isNotEqualTo(wanted)
	}

	@Test
	fun `one prefix sent as a bare value reads as a list of one`() {
		val read = IndexDefinition.read(
			mapOf("index_definition" to mapOf("prefixes" to "Probe-"), "attributes" to emptyList<Any?>()),
		)

		assertThat(read?.prefixes).containsExactly("Probe-")
	}

	@Test
	fun `a RESP3 attribute is read by name, its compared flags only`() {
		val attribute = mapOf(
			"identifier" to "$.n",
			"attribute" to "n",
			"type" to "TEXT",
			"flags" to listOf("SORTABLE", "WEIGHT"),
		)
		val read = IndexDefinition.read(
			mapOf("index_definition" to mapOf("prefixes" to listOf("P-")), "attributes" to listOf(attribute)),
		)

		assertThat(read?.attributes).containsExactly(IndexDefinition.Attribute("$.n", "n", "TEXT", setOf("SORTABLE")))
	}

	@Test
	fun `each field type is created as its RediSearch type, an array member by member`() {
		val fields = listOf(
			RedisIndexField("at", RedisFieldType.GEO),
			RedisIndexField("weight", RedisFieldType.NUMERIC),
			RedisIndexField.array("tags", RedisFieldType.TAG),
		)

		val attributes = IndexDefinition.of("Probe", fields, IndexSettings(IndexLanguage.ENGLISH, null)).attributes

		assertThat(attributes).containsExactly(
			IndexDefinition.Attribute("$.at", "at", "GEO", emptySet()),
			IndexDefinition.Attribute("$.weight", "weight", "NUMERIC", setOf("SORTABLE")),
			IndexDefinition.Attribute("$.tags[*]", "tags", "TAG", emptySet()),
		)
	}
}
