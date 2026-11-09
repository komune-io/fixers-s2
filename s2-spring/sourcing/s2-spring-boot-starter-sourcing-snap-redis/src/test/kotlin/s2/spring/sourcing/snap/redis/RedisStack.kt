package s2.spring.sourcing.snap.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.protocol.CommandArgs
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

/** One Redis Stack (RedisJSON and RediSearch) for the whole test run, started on first use. */
object RedisStack {
	private const val PORT = 6379
	private const val IMAGE =
		"redis/redis-stack:6.2.2-v2-edge@sha256:eb0bd59d722b089875c878e88b92bd6de79033242b1e4da8b6e8d789fc284df1"

	private val container: GenericContainer<*> by lazy {
		GenericContainer(DockerImageName.parse(IMAGE)).withExposedPorts(PORT).also { it.start() }
	}

	val uri: RedisURI by lazy {
		RedisURI.builder().withHost(container.host).withPort(container.getMappedPort(PORT)).build()
	}

	val client: RedisClient by lazy { RedisClient.create(uri) }

	val connection: StatefulRedisConnection<String, String> by lazy { client.connect() }

	val settings = IndexSettings(IndexLanguage.FRENCH, PhoneticMatcher.FRENCH)

	fun view(
		connection: StatefulRedisConnection<String, String> = this.connection,
		settings: IndexSettings = this.settings,
	) =
		RedisSnapView(connection, settings)
}

/** Drops a probe index, never its documents; an index already absent is not an error. */
internal suspend inline fun <reified MODEL> RedisSnapView.dropIndex() {
	try {
		dispatch(SearchCommand.FT_DROPINDEX, CommandArgs(StringCodec.UTF8).add(checkNotNull(MODEL::class.simpleName)))
	} catch (e: Exception) {
		if (!e.isIndexAbsent()) throw e
	}
}
