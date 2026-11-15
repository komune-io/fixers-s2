package s2.spring.sourcing.snap.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisCommandExecutionException
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.protocol.CommandArgs
import kotlin.reflect.KClass
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

/**
 * One Redis Stack (RedisJSON and RediSearch) for the whole test run, started on first use: the image the build
 * names in `s2.redis.image`, each supported version having its own test task.
 */
object RedisStack {
    private const val PORT = 6379

    private val image: String = checkNotNull(System.getProperty("s2.redis.image")) { "s2.redis.image is not set" }

    /**
     * Whether this RediSearch says when a prefix expansion cut an answer: from Redis Stack 7.4 on; 6.2 cuts it
     * without a word, which no refusal can then catch.
     */
    val isWarningOnCutExpansion: Boolean = !image.startsWith("redis/redis-stack:6.")

    private val container: GenericContainer<*> by lazy {
        GenericContainer(DockerImageName.parse(image)).withExposedPorts(PORT).apply { start() }
    }

    val uri: RedisURI by lazy {
        RedisURI.builder().withHost(container.host).withPort(container.getMappedPort(PORT)).build()
    }

    val client: RedisClient by lazy { RedisClient.create(uri) }

    val connection: StatefulRedisConnection<String, String> by lazy { client.connect() }

    val settings = IndexSettings(PhoneticMatcher.FRENCH)

    fun <MODEL : Any> view(
        model: KClass<MODEL>,
        fields: List<RedisIndexField>,
        idOf: (MODEL) -> String,
        connection: StatefulRedisConnection<String, String> = this.connection,
        settings: IndexSettings = this.settings,
    ) = RedisSnapView(
        connection = connection,
        settings = settings,
        model = model,
        fields = fields,
        idOf = idOf,
    )
}

/** Drops a probe index, never its documents; an absent index is no error. */
internal suspend fun RedisSnapView<*>.dropIndex() {
    try {
        index.dispatch(SearchCommand.FT_DROPINDEX, CommandArgs(StringCodec.UTF8).add(indexName))
    } catch (e: RedisCommandExecutionException) {
        if (!e.isIndexAbsent()) throw e
    }
}
