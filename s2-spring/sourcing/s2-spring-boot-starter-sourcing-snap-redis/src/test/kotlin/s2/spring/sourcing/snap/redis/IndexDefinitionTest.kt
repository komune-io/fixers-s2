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
		val definition = IndexDefinition.of("Probe", fields, IndexSettings(PhoneticMatcher.FRENCH))

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
		assertThat(IndexDefinition.of("Probe", fields, IndexSettings(null)).fingerprint).doesNotContain("LANGUAGE")
	}

	@Test
	fun `a change of phonetic matcher changes the definition`() {
		val french = IndexDefinition.of("Probe", fields, IndexSettings(PhoneticMatcher.FRENCH))
		val english = IndexDefinition.of("Probe", fields, IndexSettings(PhoneticMatcher.ENGLISH))
		val none = IndexDefinition.of("Probe", fields, IndexSettings(null))

		assertThat(setOf(french.fingerprint, english.fingerprint, none.fingerprint)).hasSize(3)
		assertThat(none.fingerprint).doesNotContain("PHONETIC")
	}

	@Test
	fun `an index names each field once, and at least one`() {
		val twice = fields + RedisIndexField("name", RedisFieldType.TAG)

		assertThatThrownBy { IndexDefinition.of("Probe", twice, IndexSettings(null)) }
			.isInstanceOf(IllegalArgumentException::class.java)
			.hasMessageContaining("names a field twice")
		assertThatThrownBy { IndexDefinition.of("Probe", emptyList(), IndexSettings(null)) }
			.isInstanceOf(IllegalArgumentException::class.java)
			.hasMessageContaining("at least one field")
	}
}
