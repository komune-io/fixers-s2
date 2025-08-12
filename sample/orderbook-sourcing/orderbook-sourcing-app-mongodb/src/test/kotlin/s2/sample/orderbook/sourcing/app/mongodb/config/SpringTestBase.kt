package s2.sample.orderbook.sourcing.app.mongodb.config

import com.redis.testcontainers.RedisContainer
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import s2.sample.orderbook.sourcing.app.mongodb.SubAutomateMongodbApp

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [SpringTestBase.Initializer::class])
@SpringBootTest(classes = [SubAutomateMongodbApp::class])
abstract class SpringTestBase {

	companion object {
		val mongoContainer: MongoDBContainer =
			MongoDBContainer(DockerImageName.parse("mongo:4.4"))

		var redisContainer =
			RedisContainer(DockerImageName.parse("redis/redis-stack:latest"));

	}

	internal class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
		override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
			if (!redisContainer.isRunning) {
				redisContainer.start()
			}
			if (!mongoContainer.isRunning) {
				mongoContainer.start()
			}
			TestPropertyValues.of(
				"spring.data.mongodb.uri=${mongoContainer.replicaSetUrl}",
				"spring.data.redis.host=${redisContainer.host}",
				"spring.data.redis.port=${redisContainer.firstMappedPort}"
			).applyTo(configurableApplicationContext.environment)
		}


	}
}
