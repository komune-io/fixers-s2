package s2.spring.sourcing.snap.redis

/**
 * RediSearch's default result window (`MAXSEARCHRESULTS`, which nothing here raises): one search returns no
 * more than this many documents, whatever its total says.
 */
const val SEARCH_WINDOW = 10_000

/**
 * Identifiers one tag union names at most. A reader of the whole store asks in slices of it, so that each
 * search stays within [SEARCH_WINDOW] as it did when each element was searched alone.
 */
const val TAG_UNION_BATCH = 500

/** Keys a scan step visits, and keys one `JSON.MGET` reads, when the store is read by key. */
const val STORE_BATCH = 1_000

/** RediSearch's default `MINPREFIX`: a shorter prefix expands to nothing. */
const val MIN_PREFIX = 2

/** A tag clause matching [value]. */
fun tagClause(field: String, value: String): String = tagClause(field, listOf(value))

/** A tag clause matching any of [values]: the `|` between them is the only query syntax it writes. */
fun tagClause(field: String, values: Collection<String>): String =
	"@${field}:{${values.joinToString("|") { it.replace(TAG_SYNTAX, "\\\\$0") }}}"

/** Inside a tag's braces RediSearch reads every punctuation mark and space as syntax unless escaped. */
private val TAG_SYNTAX = Regex("[^\\p{L}\\p{M}\\p{N}_]")

/**
 * The words of a typed text, each one a prefix from [MIN_PREFIX] letters on: whatever query syntax the text
 * holds is read as a word separator, so no text can break or widen the query.
 */
fun String?.addWildcard(): String =
	orEmpty()
		.split(QUERY_WORD_SEPARATOR)
		.filter { it.isNotEmpty() }
		.joinToString(" ") { if (it.length < MIN_PREFIX) it else "$it*" }

private val QUERY_WORD_SEPARATOR = Regex("[^\\p{L}\\p{M}\\p{N}_]+")
