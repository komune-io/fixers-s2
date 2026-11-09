package s2.spring.sourcing.snap.redis

import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

/** How long a start waits, at most and for all its indexes together, for them to read the stored documents. */
const val INDEXING_CEILING_NS = 60_000_000_000L

private const val INDEXING_POLL_MS = 100L

/**
 * One start's indexes: each view makes its own, then the start polls every one still reading at each tick,
 * against one deadline counted from the start itself, so that no index is left created but unread.
 */
class IndexingStart(
	private val deadline: Long = System.nanoTime() + INDEXING_CEILING_NS,
) : AbstractCoroutineContextElement(IndexingStart) {
	companion object Key : CoroutineContext.Key<IndexingStart> {
		private val logger = LoggerFactory.getLogger(IndexingStart::class.java)
	}

	private class Made(val view: RedisSnapView, val indexName: String)

	private val made = ConcurrentLinkedQueue<Made>()

	/** The indexes made in this start, not yet waited for. */
	val pending: List<String> get() = made.map { it.indexName }

	fun made(view: RedisSnapView, indexName: String) {
		made += Made(view, indexName)
	}

	suspend fun awaitAll() {
		var reading = made.toList()
		made.clear()
		while (true) {
			reading = reading.filterNot { it.view.indexed(it.indexName) }
			if (reading.isEmpty() || System.nanoTime() - deadline >= 0) break
			delay(INDEXING_POLL_MS)
		}
		reading.forEach { logger.warn("Index[${it.indexName}] still indexing past the start's deadline") }
	}
}
