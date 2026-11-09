package s2.spring.sourcing.snap.redis

/**
 * How every index of a [RedisSnapView] reads text. Both are chosen by the consumer, none is assumed: a
 * language stems the words of one language only, and a phonetic matcher hears one language only.
 */
class IndexSettings(
	val language: IndexLanguage,
	/** The phonetic matcher of every TEXT attribute; `null` for none. */
	val phonetic: PhoneticMatcher?,
)

/** RediSearch's stemming languages, as `FT.CREATE … LANGUAGE` takes them and `FT.INFO` reads them back. */
enum class IndexLanguage(val value: String) {
	ARABIC("arabic"),
	ARMENIAN("armenian"),
	BASQUE("basque"),
	CATALAN("catalan"),
	CHINESE("chinese"),
	DANISH("danish"),
	DUTCH("dutch"),
	ENGLISH("english"),
	FINNISH("finnish"),
	FRENCH("french"),
	GERMAN("german"),
	GREEK("greek"),
	HINDI("hindi"),
	HUNGARIAN("hungarian"),
	INDONESIAN("indonesian"),
	IRISH("irish"),
	ITALIAN("italian"),
	LITHUANIAN("lithuanian"),
	NEPALI("nepali"),
	NORWEGIAN("norwegian"),
	PORTUGUESE("portuguese"),
	ROMANIAN("romanian"),
	RUSSIAN("russian"),
	SERBIAN("serbian"),
	SPANISH("spanish"),
	SWEDISH("swedish"),
	TAMIL("tamil"),
	TURKISH("turkish"),
	YIDDISH("yiddish"),
}

/** RediSearch's Double Metaphone matchers, as `FT.CREATE … PHONETIC` takes them. */
enum class PhoneticMatcher(val value: String) {
	ENGLISH("dm:en"),
	FRENCH("dm:fr"),
	PORTUGUESE("dm:pt"),
	SPANISH("dm:es"),
}
