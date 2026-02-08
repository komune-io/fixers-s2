package s2.sample.orderbook.sourcing.app.r2dbc.config

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.support.ConnectionPoolSupport
import org.apache.commons.pool2.impl.GenericObjectPool
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RedisConfig {

	companion object {
		private const val DEFAULT_REDIS_PORT = 6379
		private const val DEFAULT_POOL_SIZE = 8
	}

	@Value("\${spring.data.redis.host:localhost}")
	private lateinit var redisHost: String

	@Suppress("MagicNumber")
	@Value("\${spring.data.redis.port:6379}")
	private var redisPort: Int = DEFAULT_REDIS_PORT

	@Bean
	fun redisClient(): RedisClient {
		return RedisClient.create("redis://$redisHost:$redisPort")
	}

	@Bean
	fun redisConnectionPool(
		client: RedisClient
	): GenericObjectPool<StatefulRedisConnection<String, String>> {
		val poolConfig = GenericObjectPoolConfig<StatefulRedisConnection<String, String>>()
		poolConfig.maxTotal = DEFAULT_POOL_SIZE
		poolConfig.maxIdle = DEFAULT_POOL_SIZE
		poolConfig.minIdle = 0
		return ConnectionPoolSupport.createGenericObjectPool(
			{ client.connect() },
			poolConfig
		)
	}
}
