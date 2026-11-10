package s2.spring.sourcing.snap.redis

/**
 * Rows one search answers at most here, whatever its total says: a whole read past it is refused, and so is a
 * larger page. It is the `MAXSEARCHRESULTS` Redis Stack answers (`FT.CONFIG GET MAXSEARCHRESULTS`: 10000); a server
 * set lower answers fewer rows than counted, which a whole read refuses as short.
 */
const val SEARCH_WINDOW = 10_000

/** Keys a scan step visits, and keys one `JSON.MGET` or `UNLINK` takes, when the store is read by key. */
const val STORE_BATCH = 1_000

/** RediSearch's default `MINPREFIX`: a shorter prefix expands to nothing. */
const val MIN_PREFIX = 2

/**
 * A tag clause matching [value].
 *
 * @throws IllegalArgumentException as [tagClause] of a list does.
 */
fun tagClause(field: String, value: String): String = tagClause(field, listOf(value))

/**
 * A tag clause matching any of [values]: the `|` between them is the only query syntax it writes.
 *
 * @throws IllegalArgumentException when [field] is not an attribute name, [values] is empty or holds an empty
 * value: a mistake of the calling code, which builds the clause, never of what a user typed.
 */
fun tagClause(field: String, values: Collection<String>): String {
    require(FIELD.matches(field)) { "Not an index attribute name: «$field»" }
    require(values.isNotEmpty()) { "A tag clause on @$field needs at least one value" }
    require(values.none { it.isEmpty() }) { "A tag clause on @$field cannot match an empty value" }
    return "@${field}:{${values.joinToString("|") { it.replace(TAG_SYNTAX, "\\\\$0") }}}"
}

private val FIELD = Regex("[A-Za-z_][A-Za-z0-9_]*")

// Inside a tag's braces RediSearch reads every punctuation mark and space as syntax unless escaped.
private val TAG_SYNTAX = Regex("""[^\p{L}\p{M}\p{N}_]""")

/**
 * The words of a typed text, each one a prefix from [MIN_PREFIX] letters on: whatever query syntax the text
 * holds is read as a word separator, so no text can break or widen the query. A prefix that expands to more terms
 * than the server's `MAXEXPANSIONS` makes the search refused as [SearchRefusal.TOO_BROAD], never cut.
 */
fun String?.addWildcard(): String =
    orEmpty()
        .split(QUERY_WORD_SEPARATOR)
        .filter { it.isNotEmpty() }
        .joinToString(" ") { if (it.length < MIN_PREFIX) it else "$it*" }

private val QUERY_WORD_SEPARATOR = Regex("""[^\p{L}\p{M}\p{N}_]+""")
