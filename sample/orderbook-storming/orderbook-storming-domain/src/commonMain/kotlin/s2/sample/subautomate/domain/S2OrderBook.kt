package s2.sample.subautomate.domain

import kotlinx.serialization.Serializable
import s2.dsl.automate.S2Role
import s2.dsl.automate.S2State
import s2.dsl.automate.event.storming.builder.s2Storming
import s2.sample.subautomate.domain.model.OrderBookId
import s2.sample.subautomate.domain.orderBook.OrderBookClosedEvent
import s2.sample.subautomate.domain.orderBook.OrderBookCreatedEvent
import s2.sample.subautomate.domain.orderBook.OrderBookPublishedEvent
import s2.sample.subautomate.domain.orderBook.OrderBookUpdatedEvent

object Role: S2Role

@Serializable
enum class OrderBookState(override var position: Int): S2State {
    Created(0),
    Published(1),
    Closed(2)
}

val orderBookAutomate = s2Storming<OrderBookId> {
    name = "S2OrderBook"
    transaction<OrderBookCreatedEvent> {
        to = OrderBookState.Created
        role = Role
    }
    selfTransaction<OrderBookUpdatedEvent> {
        states += OrderBookState.Created
        role = Role
    }
    transaction<OrderBookPublishedEvent> {
        from = OrderBookState.Created
        to = OrderBookState.Published
        role = Role
    }
    transaction<OrderBookClosedEvent> {
        from = OrderBookState.Created
        to = OrderBookState.Closed
        role = Role
    }
    transaction<OrderBookClosedEvent> {
        from = OrderBookState.Published
        to = OrderBookState.Closed
        role = Role
    }
}
