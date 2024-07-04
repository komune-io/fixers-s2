package s2.sample.orderbook.sourcing.app.r2dbc.config

import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import s2.sample.orderbook.sourcing.app.r2dbc.OrderBookModelView
import s2.sample.orderbook.sourcing.app.r2dbc.OrderBookSnapView
import s2.sample.subautomate.domain.OrderBookState
import s2.sample.subautomate.domain.model.OrderBook
import s2.sample.subautomate.domain.model.OrderBookId
import s2.sample.subautomate.domain.orderBook.OrderBookEvent
import s2.sample.subautomate.domain.orderBookAutomate
import s2.spring.automate.sourcing.S2AutomateDeciderSpring
import s2.spring.sourcing.data.S2SourcingSpringDataAdapter

@Configuration
class OrderBookAutomateConfig(
    orderBookS2Aggregate: OrderBookS2Aggregate,
    orderBookSnapView: OrderBookSnapView
): S2SourcingSpringDataAdapter<OrderBook, OrderBookState, OrderBookEvent, OrderBookId, OrderBookS2Aggregate>(
	orderBookS2Aggregate, OrderBookModelView(), orderBookSnapView
) {
	override fun automate() = orderBookAutomate("sourcing-r2dbc")
	override fun entityType() = OrderBookEvent::class
}

@Service
class OrderBookS2Aggregate : S2AutomateDeciderSpring<OrderBook, OrderBookState, OrderBookEvent, OrderBookId>()
