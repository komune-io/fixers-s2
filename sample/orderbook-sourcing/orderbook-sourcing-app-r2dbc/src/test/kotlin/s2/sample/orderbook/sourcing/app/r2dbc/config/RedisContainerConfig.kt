package s2.sample.orderbook.sourcing.app.r2dbc.config

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.GenericContainer

class RedisContainerConfig {
    companion object {
        val redisContainer = GenericContainer<Nothing>("redis/redis-stack:6.2.2-v2-edge")
            .apply { withExposedPorts(6379) }
    }

    internal class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            redisContainer.start()
            TestPropertyValues.of(
                "spring.data.redis.host=${redisContainer.host}",
                "spring.data.redis.port=${redisContainer.firstMappedPort}"
            ).applyTo(configurableApplicationContext.environment)
        }
    }
}
