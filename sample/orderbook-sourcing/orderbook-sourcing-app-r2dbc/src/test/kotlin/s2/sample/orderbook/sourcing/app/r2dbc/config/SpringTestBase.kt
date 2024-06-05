package s2.sample.orderbook.sourcing.app.r2dbc.config

import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import s2.sample.orderbook.sourcing.app.r2dbc.SubAutomateR2dbcApp

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension::class)
@Import(ContainersConfig::class)
@ContextConfiguration(initializers = [RedisContainerConfig.Initializer::class])
@SpringBootTest(classes = [SubAutomateR2dbcApp::class])
abstract class SpringTestBase
