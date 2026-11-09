package s2.spring.sourcing.snap.redis

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.SmartInitializingSingleton

/**
 * Creates every view's index once all beans exist: before the web server, the scheduled tasks and every
 * runner, which all search the views.
 */
class RedisIndexes(private val indexes: List<RedisIndexInitializer>) : SmartInitializingSingleton {

	override fun afterSingletonsInstantiated() = runBlocking { create() }

	/** Makes every index, then waits for them all together: see [IndexingStart]. */
	suspend fun create() {
		val start = IndexingStart()
		withContext(start) { indexes.forEach { it.initIndex() } }
		start.awaitAll()
	}
}
