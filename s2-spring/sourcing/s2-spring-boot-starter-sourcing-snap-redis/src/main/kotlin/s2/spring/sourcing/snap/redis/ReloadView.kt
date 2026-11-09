package s2.spring.sourcing.snap.redis

import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KClass
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
	private val model: KClass<MODEL>,
) {
	companion object {
		private val logger = LoggerFactory.getLogger(ReloadView::class.java)

		inline operator fun <reified MODEL : Any> invoke(
			view: RedisSnapRepository<MODEL>,
			decider: S2AutomateSourcingDecider<*, *, *, *>,
		): ReloadView<MODEL> = ReloadView(view, decider, MODEL::class)
	}

	/**
	 * Replays the history into the view when it stores no snapshot or [forceReload] is set, emptying it first.
	 * Emptiness is counted by key, not through the index, which may hold fewer. A failed replay empties the view,
	 * then rethrows; a view that cannot be emptied adds that failure to it as suppressed.
	 */
	suspend fun reload(forceReload: Boolean = false) {
		val name = model.java.name
		val count = view.storedCount()
		if (!forceReload && count != 0L) return
		if (count != 0L) {
			val deleted = view.deleteAll()
			logger.info("Replay - {} - deleted {} snapshots", name, deleted)
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

	private suspend fun empty(failure: Exception) {
		try {
			view.deleteAll()
		} catch (e: CancellationException) {
			throw e
		} catch (e: Exception) {
			failure.addSuppressed(e)
		}
	}
}
