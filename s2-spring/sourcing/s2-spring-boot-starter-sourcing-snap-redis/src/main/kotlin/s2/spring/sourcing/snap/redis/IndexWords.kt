package s2.spring.sourcing.snap.redis

import java.text.Normalizer

/**
 * A name read the way the search index reads a term, for names the index does not hold (read from another
 * service): every word of the term opens a word of the name — its start from [MIN_PREFIX] letters on, the
 * whole word below — accents and case aside.
 */
object IndexWords {
	fun holds(name: String, term: String): Boolean {
		val wanted = words(term)
		if (wanted.isEmpty()) return false
		val held = words(name)
		return wanted.all { word -> held.any { candidate -> candidate.opens(word) } }
	}

	private fun String.opens(word: String): Boolean = if (word.length < MIN_PREFIX) this == word else startsWith(word)

	private fun words(text: String): List<String> =
		Normalizer.normalize(text, Normalizer.Form.NFD)
			.replace(DIACRITICS, "")
			.lowercase()
			.split(SEPARATOR)
			.filter { it.isNotEmpty() }

	private val DIACRITICS = Regex("\\p{M}+")
	private val SEPARATOR = Regex("[^\\p{L}\\p{N}]+")
}
