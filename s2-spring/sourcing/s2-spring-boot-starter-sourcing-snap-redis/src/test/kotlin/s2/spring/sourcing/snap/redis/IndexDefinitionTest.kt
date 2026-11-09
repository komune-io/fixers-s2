package s2.spring.sourcing.snap.redis

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class IndexDefinitionTest {

	private fun resp2(vararg attributes: List<Any?>) = mapOf(
		"index_definition" to listOf("key_type", "JSON", "default_language", "english"),
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
			mapOf("index_definition" to listOf("key_type", "JSON"), "attributes" to emptyList<Any?>()),
		)

		assertThat(read?.language).isEqualTo(IndexLanguage.ENGLISH.value)
	}

	@Test
	fun `an answer that cannot be read is no definition, not an empty one`() {
		assertThat(IndexDefinition.read(mapOf("index_definition" to listOf("default_language", "english")))).isNull()
		assertThat(IndexDefinition.read(mapOf("attributes" to emptyList<Any?>()))).isNull()
		assertThat(IndexDefinition.read(resp2(listOf("attribute", "id", "identifier", "$.id")))).isNull()
	}

	@Test
	fun `the creation sends the flags the definition holds, and only those`() {
		val bare = IndexDefinition("english", false, listOf(IndexDefinition.Attribute("$.n", "n", "TEXT", setOf("NOSTEM"))))

		val sent = bare.createArgs("Probe").toCommandString()

		assertThat(sent).contains("NOSTEM").doesNotContain("SORTABLE").doesNotContain("PHONETIC").contains("STOPWORDS 0")
	}

	@Test
	fun `the language and the phonetic matcher are the settings' own`() {
		val sent = IndexDefinition.of(name, IndexSettings(IndexLanguage.SPANISH, PhoneticMatcher.SPANISH))
			.createArgs("Probe").toCommandString()

		assertThat(sent).contains("LANGUAGE spanish").contains("PHONETIC dm:es")
	}

	@Test
	fun `no phonetic matcher sends none`() {
		val sent = IndexDefinition.of(name, IndexSettings(IndexLanguage.ENGLISH, null)).createArgs("Probe").toCommandString()

		assertThat(sent).contains("LANGUAGE english").doesNotContain("PHONETIC")
	}

	@Test
	fun `two definitions in different languages differ, so the index is made again`() {
		val english = IndexDefinition.of(name, IndexSettings(IndexLanguage.ENGLISH, null))
		val french = IndexDefinition.of(name, IndexSettings(IndexLanguage.FRENCH, null))

		assertThat(english).isNotEqualTo(french)
	}

	@Test
	fun `the phonetic matcher, which FT INFO does not show, is not compared`() {
		val with = IndexDefinition.of(name, IndexSettings(IndexLanguage.ENGLISH, PhoneticMatcher.ENGLISH))
		val without = IndexDefinition.of(name, IndexSettings(IndexLanguage.ENGLISH, null))

		assertThat(with).isEqualTo(without).hasSameHashCodeAs(without)
	}
}
