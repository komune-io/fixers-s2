package s2.spring.sourcing.snap.redis

/** A view that makes its own index; [RedisIndexes] calls every one at start. */
interface RedisIndexInitializer {
    /** The index this view makes, unique among a start's views. */
    val indexName: String

    /**
     * Makes the index match its definition and waits until it has read the stored documents; called again, it
     * changes nothing.
     */
    suspend fun initIndex()
}
