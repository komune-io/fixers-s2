package s2.spring.sourcing.snap.redis

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.SmartInitializingSingleton

/**
 * Makes every view's index once all beans exist: before the web server, the scheduled tasks and every runner,
 * which all search the views. A view that cannot make or read its index fails the start.
 */
class RedisIndexes(private val indexes: List<RedisIndexInitializer>) : SmartInitializingSingleton {
    init {
        val shared = indexes.groupBy { it.indexName }.filterValues { it.size > 1 }.keys
        require(shared.isEmpty()) { "Two views make the same index, their models sharing a name: $shared" }
    }

    override fun afterSingletonsInstantiated() {
        runBlocking { create() }
    }

    /** Makes every index at once and waits for them all; the first failure cancels the others and is thrown. */
    suspend fun create() {
        coroutineScope {
            indexes.forEach { index -> launch { index.initIndex() } }
        }
    }
}
