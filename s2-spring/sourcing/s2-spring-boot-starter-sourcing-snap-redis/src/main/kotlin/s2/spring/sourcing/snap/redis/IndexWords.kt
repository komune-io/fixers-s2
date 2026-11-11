package s2.spring.sourcing.snap.redis

import java.text.Normalizer

/**
 * A name read the way the search index reads a term, for names the index does not hold (read from another
 * service): every word of the term opens a word of the name — its start from `minPrefix` letters on, the
 * whole word below — accents and case aside.
 */
object IndexWords {
    private val DIACRITICS = Regex("\\p{M}+")

    /** Whether [name] holds [term] as a search with words made prefixes from [minPrefix] letters would find it. */
    fun holds(name: String, term: String, minPrefix: Int = MIN_PREFIX): Boolean {
        val wanted = words(term)
        if (wanted.isEmpty()) return false
        val held = words(name)
        return wanted.all { word -> held.any { candidate -> candidate.opens(word = word, minPrefix = minPrefix) } }
    }

    private fun String.opens(word: String, minPrefix: Int): Boolean =
        if (word.length < minPrefix) this == word else startsWith(word)

    private fun words(text: String): List<String> =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .lowercase()
            .split(WORD_SEPARATOR)
            .filter { it.isNotEmpty() }
}
