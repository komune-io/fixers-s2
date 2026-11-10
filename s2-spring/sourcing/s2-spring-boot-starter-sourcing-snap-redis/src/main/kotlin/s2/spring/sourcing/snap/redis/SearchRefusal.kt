package s2.spring.sourcing.snap.redis

import f2.dsl.cqrs.error.F2Error
import f2.dsl.cqrs.error.F2Refusal
import f2.dsl.cqrs.error.HttpRefusal
import f2.dsl.cqrs.exception.F2Exception

/**
 * The refusals of a search that cannot answer what it was asked without cutting it: a cut list would pass the
 * missing rows off as non-existent. Each carries the figures that broke the rule, never the index nor the store's
 * own wording, which the server logs.
 */
object SearchRefusal {
    /** More rows than one read answers at once: the read has to be paged. */
    const val TOO_MANY_ENTRIES = "search.TOO_MANY_ENTRIES"

    /** Fewer rows answered than counted, a whole read: worth one more try, then an unreadable entry to report. */
    const val SHORT_READ = "search.SHORT_READ"

    /** A prefix of the query expanded past the store's `MAXEXPANSIONS`, so its answer was cut: a longer prefix. */
    const val TOO_BROAD = "search.TOO_BROAD"

    /** The store answered past its query timeout, with part of the rows: worth another try. */
    const val TIMED_OUT = "search.TIMED_OUT"

    /** A page larger than one read answers. */
    const val PAGE_TOO_LARGE = "search.PAGE_TOO_LARGE"

    const val TOTAL = "total"
    const val BOUND = "bound"
    const val READ = "read"
    const val LIMIT = "limit"

    fun tooManyEntries(total: Int, bound: Int) = refusal(
        code = TOO_MANY_ENTRIES,
        status = HttpRefusal.UNPROCESSABLE,
        message = "$total entries, past the $bound one read answers at once",
        parameters = mapOf(TOTAL to "$total", BOUND to "$bound"),
    )

    fun shortRead(total: Int, read: Int) = refusal(
        code = SHORT_READ,
        status = HttpRefusal.CONFLICT,
        message = "$total entries announced, $read answered",
        parameters = mapOf(TOTAL to "$total", READ to "$read"),
    )

    fun tooBroad() = refusal(
        code = TOO_BROAD,
        status = HttpRefusal.UNPROCESSABLE,
        message = "A word of the search matches too many terms: its answer would be cut",
        parameters = emptyMap(),
    )

    fun timedOut() = refusal(
        code = TIMED_OUT,
        status = HttpRefusal.UNAVAILABLE,
        message = "The search took too long and answered part of its rows",
        parameters = emptyMap(),
    )

    fun pageTooLarge(limit: Int, bound: Int) = refusal(
        code = PAGE_TOO_LARGE,
        status = HttpRefusal.UNPROCESSABLE,
        message = "A page of $limit rows, past the $bound one read answers at once",
        parameters = mapOf(LIMIT to "$limit", BOUND to "$bound"),
    )

    private fun refusal(code: String, status: HttpRefusal, message: String, parameters: Map<String, String>) =
        F2Exception(F2Error(message = message, code = status.status, refusal = F2Refusal(code, parameters)))
}

/** RediSearch's warning when a prefix expanded past `MAXEXPANSIONS` (Redis Stack 7.4 and later, RESP3 only). */
internal const val EXPANSIONS_WARNING = "Max prefix expansions limit was reached"

/** RediSearch's warning when a query ran past its timeout and answered part of its rows. */
internal const val TIMEOUT_WARNING = "Timeout limit was reached"

/**
 * The refusal for a search answer carrying [warning]: a cut by expansion or a timeout. Any other warning is a store
 * this module does not know, an error rather than a guess.
 */
internal fun refusalFor(indexName: String, warning: String): F2Exception = when {
    warning.contains(EXPANSIONS_WARNING, ignoreCase = true) -> SearchRefusal.tooBroad()
    warning.contains(TIMEOUT_WARNING, ignoreCase = true) -> SearchRefusal.timedOut()
    else -> error("Index[$indexName]: the search answered an unknown warning: $warning")
}
