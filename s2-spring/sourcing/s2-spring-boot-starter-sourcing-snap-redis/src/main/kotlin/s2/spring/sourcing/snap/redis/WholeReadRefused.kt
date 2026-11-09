package s2.spring.sourcing.snap.redis

/**
 * A search asked for every row could not answer them all. A cut list would pass the missing rows off as
 * non-existent, so the read is refused instead; the consumer maps it to its own answer.
 */
sealed class WholeReadRefused(message: String) : IllegalStateException(message) {
	/** The index searched. */
	abstract val index: String

	/** Rows the search counted. */
	abstract val total: Int

	/** More rows than one search answers: the [bound] has to be raised, or the read paged. */
	class TooManyEntries(override val index: String, override val total: Int, val bound: Int) : WholeReadRefused(
		"Index[$index]: $total entries, past the $bound one read answers at once",
	)

	/** Fewer rows answered than counted: worth one more try, then an unreadable entry to report. */
	class ShortRead(override val index: String, override val total: Int, val read: Int) : WholeReadRefused(
		"Index[$index]: $total entries announced, $read answered",
	)
}
