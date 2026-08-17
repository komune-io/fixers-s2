package s2.sample.orderbook.sourcing.app.mongodb.config

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import s2.sample.orderbook.sourcing.app.mongodb.SubAutomateMongodbApp
import s2.sample.orderbook.sourcing.core.test.OrderBookSpringTestBase

@Import(ContainersConfig::class)
@SpringBootTest(classes = [SubAutomateMongodbApp::class])
abstract class SpringTestBase : OrderBookSpringTestBase()
