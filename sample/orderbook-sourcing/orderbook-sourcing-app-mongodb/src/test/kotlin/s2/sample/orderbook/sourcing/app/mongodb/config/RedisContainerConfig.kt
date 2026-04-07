package s2.sample.orderbook.sourcing.app.mongodb.config

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

class RedisContainerConfig {

    companion object {
        val redisContainer: GenericContainer<*> =
            GenericContainer(DockerImageName.parse("redis/redis-stack:latest"))
                .withExposedPorts(6379)
    }

    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            if (!redisContainer.isRunning) {
                redisContainer.start()
            }
            TestPropertyValues.of(
                "spring.data.redis.host=${redisContainer.host}",
                "spring.data.redis.port=${redisContainer.getMappedPort(6379)}"
            ).applyTo(configurableApplicationContext.environment)
        }
    }
}
