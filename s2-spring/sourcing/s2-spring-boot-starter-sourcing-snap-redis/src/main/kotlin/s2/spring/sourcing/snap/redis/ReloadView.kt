package s2.spring.sourcing.snap.redis

import kotlin.coroutines.cancellation.CancellationException
import org.slf4j.LoggerFactory
import s2.automate.core.sourcing.S2AutomateSourcingDecider

/**
 * Rebuilds a view from its event history: when it stores nothing, or always when forced. A replay that fails
 * leaves the view empty and fails the start, so that the next start replays it again instead of serving it half
 * written.
 */
class ReloadView<MODEL : Any>(
    private val view: RedisSnapRepository<MODEL>,
    private val decider: S2AutomateSourcingDecider<*, *, *, *>,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ReloadView::class.java)

        /**
         * Reloads every one of [reloads], each failure included: a failed view does not keep the next from its
         * replay. Once all have run, the first failure is thrown with the others suppressed in it.
         */
        // Every failure of a replay is kept to be rethrown, whatever its type.
        @Suppress("TooGenericExceptionCaught")
        suspend fun reloadAll(reloads: List<ReloadView<*>>, forceReload: Boolean = false) {
            val failures = reloads.mapNotNull { reload ->
                try {
                    reload.reload(forceReload)
                    null
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    e
                }
            }
            failures.firstOrNull()?.let { first ->
                failures.drop(1).forEach(first::addSuppressed)
                throw first
            }
        }
    }

    /**
     * Replays the history into the view when it stores no snapshot or [forceReload] is set, emptying it first.
     * Emptiness is read by key, not through the index. A failed replay empties the view, then rethrows; a view that
     * cannot be emptied adds that failure to it as suppressed.
     */
    // Any failure of the replay leaves the view half written: it is emptied whatever the failure, then rethrown.
    @Suppress("TooGenericExceptionCaught")
    suspend fun reload(forceReload: Boolean = false) {
        val name = view.indexName
        val empty = view.isEmpty()
        if (!forceReload && !empty) return
        if (!empty) {
            logger.info("Replay - {} - deleted {} snapshots", name, view.deleteAll())
        }
        try {
            logger.info("Replaying event history - {}", name)
            decider.replayHistory()
            logger.info("Replayed event history - {}", name)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Replay failed - {} - emptying the view so the next start replays it", name, e)
            empty(e)
            throw e
        }
    }

    // The emptying failure joins the replay's, which is the one rethrown.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun empty(failure: Exception) {
        try {
            val _ = view.deleteAll()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failure.addSuppressed(e)
        }
    }
}
