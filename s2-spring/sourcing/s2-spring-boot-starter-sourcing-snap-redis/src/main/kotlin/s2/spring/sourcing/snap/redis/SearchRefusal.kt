package s2.spring.sourcing.snap.redis

import f2.dsl.cqrs.error.F2Error
import f2.dsl.cqrs.error.HttpRefusal
import f2.dsl.cqrs.exception.F2Exception

/**
 * The refusals of a search that cannot answer what it was asked without cutting it: a cut list would pass the
 * missing rows off as non-existent. Each names the index in its parameters, with the figures that broke the rule.
 */
object SearchRefusal {
    /** More rows than one read answers at once: the read has to be paged. */
    const val TOO_MANY_ENTRIES = "search.TOO_MANY_ENTRIES"

    /** Fewer rows answered than counted, a whole read: worth one more try, then an unreadable entry to report. */
    const val SHORT_READ = "search.SHORT_READ"

    /** RediSearch answered with a warning: a prefix expanded past `MAXEXPANSIONS`, or a timeout, cut the answer. */
    const val TOO_BROAD = "search.TOO_BROAD"

    /** A page larger than one read answers. */
    const val PAGE_TOO_LARGE = "search.PAGE_TOO_LARGE"

    const val INDEX = "index"
    const val TOTAL = "total"
    const val BOUND = "bound"
    const val READ = "read"
    const val LIMIT = "limit"
    const val WARNING = "warning"

    fun tooManyEntries(index: String, total: Int, bound: Int) = refusal(
        TOO_MANY_ENTRIES,
        HttpRefusal.UNPROCESSABLE,
        "Index[$index]: $total entries, past the $bound one read answers at once",
        mapOf(INDEX to index, TOTAL to "$total", BOUND to "$bound"),
    )

    fun shortRead(index: String, total: Int, read: Int) = refusal(
        SHORT_READ,
        HttpRefusal.CONFLICT,
        "Index[$index]: $total entries announced, $read answered",
        mapOf(INDEX to index, TOTAL to "$total", READ to "$read"),
    )

    fun tooBroad(index: String, warning: String) = refusal(
        TOO_BROAD,
        HttpRefusal.UNPROCESSABLE,
        "Index[$index]: the search answered with a warning, its rows are not all there: $warning",
        mapOf(INDEX to index, WARNING to warning),
    )

    fun pageTooLarge(index: String, limit: Int, bound: Int) = refusal(
        PAGE_TOO_LARGE,
        HttpRefusal.UNPROCESSABLE,
        "Index[$index]: a page of $limit rows, past the $bound one read answers at once",
        mapOf(INDEX to index, LIMIT to "$limit", BOUND to "$bound"),
    )

    private fun refusal(code: String, status: HttpRefusal, message: String, parameters: Map<String, String>) =
        F2Exception(F2Error(message = message, code = status.status, refusal = code, parameters = parameters))
}
