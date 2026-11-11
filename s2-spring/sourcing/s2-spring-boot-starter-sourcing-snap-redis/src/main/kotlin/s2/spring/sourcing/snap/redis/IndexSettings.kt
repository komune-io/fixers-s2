package s2.spring.sourcing.snap.redis

/**
 * How every index of a [RedisSnapView] reads text. Words are never stemmed (`NOSTEM`), so no language is set; the
 * phonetic matcher, chosen by the consumer, hears one language only.
 */
data class IndexSettings(
    /** The phonetic matcher of every TEXT attribute; `null` for none. */
    val phonetic: PhoneticMatcher?,
    /**
     * The letters from which a typed word is searched as a prefix ([RedisSnapView.wildcard]); a shorter word is
     * searched whole. Two by default, RediSearch's own floor; an index holding many terms raises it, so that a short
     * prefix does not expand past `MAXEXPANSIONS` and get the search refused.
     */
    val wildcardMinPrefix: Int = MIN_PREFIX,
) {
    init {
        require(wildcardMinPrefix >= MIN_PREFIX) {
            "RediSearch expands no prefix under $MIN_PREFIX letters: $wildcardMinPrefix"
        }
    }
}

/** RediSearch's Double Metaphone matchers, as `FT.CREATE … PHONETIC` takes them. */
enum class PhoneticMatcher(val value: String) {
    ENGLISH("dm:en"),
    FRENCH("dm:fr"),
    PORTUGUESE("dm:pt"),
    SPANISH("dm:es"),
}
