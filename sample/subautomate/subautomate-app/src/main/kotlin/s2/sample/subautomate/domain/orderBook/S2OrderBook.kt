package s2.sample.subautomate.domain.orderBook

import kotlinx.serialization.Serializable
import s2.dsl.automate.S2Role
import s2.dsl.automate.S2State
import s2.dsl.automate.builder.s2
import s2.sample.subautomate.domain.order.orderAutomate

object Role: S2Role


@Serializable
enum class OrderBookState(override var position: Int): S2State {
    Created(0),
    Published(1),
    Closed(2)
}

val orderBookAutomate = s2 {
    name = "S2OrderBook"
    transaction<OrderBookCreateCommand> {
        to = OrderBookState.Created
        role = Role
    }
    selfTransaction<OrderBookUpdateCommand> {
        states += OrderBookState.Created
        role = Role
    }
    transaction<OrderBookPublishCommand> {
        from = OrderBookState.Created
        to = OrderBookState.Published
        role = Role
    }
    transaction<OrderBookCloseCommand> {
        from = OrderBookState.Created
        to = OrderBookState.Closed
        role = Role
    }
    transaction<OrderBookCloseCommand> {
        from = OrderBookState.Published
        to = OrderBookState.Closed
        role = Role
    }
    submachine {
        automate = orderAutomate
    }
}
