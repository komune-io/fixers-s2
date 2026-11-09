package s2.spring.sourcing.snap.redis

import kotlinx.coroutines.flow.Flow

/** A snapshot view kept in Redis, as [ReloadView] and a backup read and empty it. */
interface RedisSnapRepository<MODEL> {
	suspend fun all(): Flow<MODEL>

	/**
	 * Every snapshot in the store, read by key and not through the search index: the index drops a document it
	 * cannot index, and answers no more than [SEARCH_WINDOW], both without a word. A scan of the whole keyspace.
	 */
	suspend fun stored(): List<MODEL>

	suspend fun count(): Long
	suspend fun deleteAll(): Int
	suspend fun get(id: String): MODEL?
	suspend fun save(entity: MODEL): MODEL
}
