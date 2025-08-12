package s2.sample.orderbook.sourcing.app.r2dbc.config

import com.redis.testcontainers.RedisContainer
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.utility.DockerImageName


class RedisContainerConfig {
    companion object {
        var redisContainer = RedisContainer(DockerImageName.parse("redis/redis-stack:latest"));
    }


    internal class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            if (!redisContainer.isRunning) {
                redisContainer.start()
            }
            TestPropertyValues.of(
                "spring.data.redis.host=${redisContainer.host}",
                "spring.data.redis.port=${redisContainer.firstMappedPort}"
            ).applyTo(configurableApplicationContext.environment)
        }
    }
}
