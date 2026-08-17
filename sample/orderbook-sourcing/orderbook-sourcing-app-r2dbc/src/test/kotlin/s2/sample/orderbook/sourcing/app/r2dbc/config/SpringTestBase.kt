package s2.sample.orderbook.sourcing.app.r2dbc.config

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import s2.sample.orderbook.sourcing.app.r2dbc.SubAutomateR2dbcApp
import s2.sample.orderbook.sourcing.core.test.OrderBookSpringTestBase

@Import(ContainersConfig::class)
@SpringBootTest(classes = [SubAutomateR2dbcApp::class])
abstract class SpringTestBase : OrderBookSpringTestBase()
