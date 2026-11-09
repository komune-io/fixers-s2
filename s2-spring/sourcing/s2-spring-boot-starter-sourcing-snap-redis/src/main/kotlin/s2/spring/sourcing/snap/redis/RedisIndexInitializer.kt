package s2.spring.sourcing.snap.redis

/** A view that makes its own index; [RedisIndexes] calls every one at start. */
fun interface RedisIndexInitializer {
	/** Makes the index match its definition; called again, it changes nothing. */
	suspend fun initIndex()
}
