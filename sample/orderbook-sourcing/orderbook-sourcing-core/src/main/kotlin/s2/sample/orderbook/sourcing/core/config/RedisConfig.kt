package s2.sample.orderbook.sourcing.core.config

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.support.ConnectionPoolSupport
import org.apache.commons.pool2.impl.GenericObjectPool
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

// Explicitly open: this module does not apply the kotlin-spring (allopen) plugin,
// and @Configuration classes must be subclassable for CGLIB proxying.
@Configuration
open class RedisConfig(
	@Value("\${spring.data.redis.host:localhost}")
	private val redisHost: String,
	@Value("\${spring.data.redis.port:6379}")
	private val redisPort: Int,
) {

	companion object {
		private const val DEFAULT_POOL_SIZE = 8
	}

	@Bean
	open fun redisClient(): RedisClient {
		return RedisClient.create("redis://$redisHost:$redisPort")
	}

	@Bean
	open fun redisConnectionPool(
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
