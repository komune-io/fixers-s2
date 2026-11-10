package s2.spring.sourcing.snap.redis

import kotlinx.coroutines.flow.Flow

/** A snapshot view kept in Redis, as [ReloadView] and a backup read and empty it. */
interface RedisSnapRepository<MODEL : Any> {
    /** The index of the view, which also prefixes its keys. */
    val indexName: String

    /**
     * Every snapshot in the store, read by key in batches and not through the search index, which answers no more
     * than [SEARCH_WINDOW] rows. A scan of the whole keyspace; one unreadable snapshot fails the flow.
     */
    fun stored(): Flow<MODEL>

    /** Documents the search index holds. */
    suspend fun count(): Long

    /** Whether the store holds no snapshot of this view, read by key: the scan stops at the first one. */
    suspend fun isEmpty(): Boolean

    /** Deletes every snapshot of this view, read or not, in batches; answers how many there were. */
    suspend fun deleteAll(): Int
}
