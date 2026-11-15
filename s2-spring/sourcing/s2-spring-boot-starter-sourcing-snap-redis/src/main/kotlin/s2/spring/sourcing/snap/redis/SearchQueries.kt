package s2.spring.sourcing.snap.redis

import java.text.Normalizer
import java.util.Locale

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
 * The words of a typed text, folded ([foldText]), each one a prefix from [minPrefix] letters on — a shorter word is
 * searched whole:
 * whatever query syntax the text holds is read as a word separator, so no text can break or widen the query. A
 * prefix that expands to more terms than the server's `MAXEXPANSIONS` makes the search refused as
 * [SearchRefusal.TOO_BROAD], never cut: an application whose index holds many terms raises [minPrefix] (see
 * [IndexSettings.wildcardMinPrefix]).
 */
fun String?.addWildcard(minPrefix: Int = MIN_PREFIX): String {
    require(minPrefix >= MIN_PREFIX) { "RediSearch expands no prefix under $MIN_PREFIX letters: $minPrefix" }
    return foldText(orEmpty())
        .split(WORD_SEPARATOR)
        .filter { it.isNotEmpty() }
        .joinToString(" ") { if (it.length < minPrefix) it else "$it*" }
}

/** What parts the words of a text as RediSearch reads them: `_` joins a word, it does not split one. */
internal val WORD_SEPARATOR = Regex("""[^\p{L}\p{M}\p{N}_]+""")

/**
 * A text as every view's index holds it: accents and other marks taken off, then lowercased — on every Redis Stack,
 * 6.2 lowercasing no capital beyond ASCII. A search ([foldQuery]) and [IndexWords] fold alike.
 */
fun foldText(text: String): String =
    Normalizer.normalize(Normalizer.normalize(text, Normalizer.Form.NFD).replace(MARKS, ""), Normalizer.Form.NFC)
        .lowercase(Locale.ROOT)

/**
 * [query] with its words folded as [foldText] folds the texts a view's index holds, for a search of one: tag values
 * between braces, ranges between brackets, `@attribute` names and `$parameters` are kept as written, a tag keeping
 * its accents and case. [RedisSnapView.search] folds its query; a view's own `FT.SEARCH` on a text attribute must.
 */
fun foldQuery(query: String): String {
    val folded = StringBuilder(query.length)
    val words = StringBuilder()
    var i = 0
    while (i < query.length) {
        val c = query[i]
        if (c == '\\' || c !in VERBATIM_START) {
            val end = (i + if (c == '\\') 2 else 1).coerceAtMost(query.length)
            words.append(query, i, end)
            i = end
        } else {
            folded.append(foldText(words.toString()))
            words.setLength(0)
            val end = verbatimEnd(query, i)
            folded.append(query, i, end)
            i = end
        }
    }
    return folded.append(foldText(words.toString())).toString()
}

private val MARKS = Regex("\\p{M}+")

private val VERBATIM_START = setOf('{', '[', '@', '$')

private val CLOSING = mapOf('{' to '}', '[' to ']')

// Past a tag's braces or a range's brackets, escapes included, or past the names after `@` (`@a|b:`) or `$`.
private fun verbatimEnd(query: String, start: Int): Int {
    val close = CLOSING[query[start]]
    var i = start + 1
    var isOpen = true
    while (isOpen && i < query.length) {
        val c = query[i]
        when {
            c == '\\' -> {
                i += 2
            }
            close != null -> {
                i++
                isOpen = c != close
            }
            c.isLetterOrDigit() || c == '_' || c == '|' -> {
                i++
            }
            else -> {
                isOpen = false
            }
        }
    }
    return i.coerceAtMost(query.length)
}
