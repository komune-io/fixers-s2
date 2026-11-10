package s2.sample.orderbook.sourcing.core.config

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
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

    @Bean(destroyMethod = "shutdown")
    open fun redisClient(): RedisClient = RedisClient.create("redis://$redisHost:$redisPort")

    @Bean(destroyMethod = "close")
    open fun redisConnection(client: RedisClient): StatefulRedisConnection<String, String> = client.connect()
}
