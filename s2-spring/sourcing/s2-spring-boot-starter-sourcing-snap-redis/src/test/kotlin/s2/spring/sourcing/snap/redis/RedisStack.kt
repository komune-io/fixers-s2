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

/** One Redis Stack (RedisJSON and RediSearch) for the whole test run, started on first use. */
object RedisStack {
    private const val PORT = 6379
    private const val IMAGE =
        "redis/redis-stack:7.4.0-v8@sha256:5d5154123426693540ea6c1d9e638eae2bf2024879c839db6fcfdd7717e817c7"

    private val container: GenericContainer<*> by lazy {
        GenericContainer(DockerImageName.parse(IMAGE)).withExposedPorts(PORT).apply { start() }
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

/** Drops a probe index and the definition written next to it, never its documents; an absent index is no error. */
internal suspend fun RedisSnapView<*>.dropIndex() {
    try {
        index.dispatch(SearchCommand.FT_DROPINDEX, CommandArgs(StringCodec.UTF8).add(indexName))
    } catch (e: RedisCommandExecutionException) {
        if (!e.isIndexAbsent()) throw e
    }
    RedisStack.connection.sync().del("$indexName:definition")
}
