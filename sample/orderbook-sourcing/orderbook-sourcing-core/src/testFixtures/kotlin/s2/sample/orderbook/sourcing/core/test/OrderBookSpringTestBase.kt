package s2.sample.orderbook.sourcing.core.test

import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * Shared Spring test scaffolding for the orderbook sample apps: JUnit lifecycle,
 * Spring extension and the Redis testcontainer. Each app subclass adds its own
 * `@SpringBootTest(classes = [...])` and database container `@Import`.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [RedisContainerConfig.Initializer::class])
abstract class OrderBookSpringTestBase
