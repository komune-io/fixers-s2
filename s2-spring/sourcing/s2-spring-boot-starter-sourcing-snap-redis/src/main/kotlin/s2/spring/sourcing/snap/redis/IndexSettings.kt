package s2.spring.sourcing.snap.redis

/**
 * How every index of a [RedisSnapView] reads text. Words are never stemmed (`NOSTEM`), so no language is set; the
 * phonetic matcher, chosen by the consumer, hears one language only.
 */
class IndexSettings(
	/** The phonetic matcher of every TEXT attribute; `null` for none. */
	val phonetic: PhoneticMatcher?,
)

/** RediSearch's Double Metaphone matchers, as `FT.CREATE … PHONETIC` takes them. */
enum class PhoneticMatcher(val value: String) {
	ENGLISH("dm:en"),
	FRENCH("dm:fr"),
	PORTUGUESE("dm:pt"),
	SPANISH("dm:es"),
}
