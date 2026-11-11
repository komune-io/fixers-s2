package s2.sample.orderbook.sourcing.app.mongodb

import f2.dsl.fnc.invoke
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import s2.sample.orderbook.sourcing.app.mongodb.config.SpringTestBase
import s2.sample.orderbook.sourcing.core.OrderBookDeciderImpl
import s2.sample.subautomate.domain.OrderBookState
import s2.sample.subautomate.domain.model.OrderBook
import s2.sample.subautomate.domain.model.OrderBookId
import s2.sample.subautomate.domain.orderbook.OrderBookCloseCommand
import s2.sample.subautomate.domain.orderbook.OrderBookClosedEvent
import s2.sample.subautomate.domain.orderbook.OrderBookCreateCommand
import s2.sample.subautomate.domain.orderbook.OrderBookCreatedEvent
import s2.sample.subautomate.domain.orderbook.OrderBookDecide
import s2.sample.subautomate.domain.orderbook.OrderBookEvent
import s2.sample.subautomate.domain.orderbook.OrderBookPublishCommand
import s2.sample.subautomate.domain.orderbook.OrderBookPublishedEvent
import s2.sample.subautomate.domain.orderbook.OrderBookUpdateCommand
import s2.sample.subautomate.domain.orderbook.OrderBookUpdatedEvent
import s2.sourcing.dsl.Loader
import s2.sourcing.dsl.event.EventRepository

internal class SubAutomateMongodbAppTest: SpringTestBase() {

    @Autowired
    lateinit var create: OrderBookDecide<OrderBookCreateCommand, OrderBookCreatedEvent>

    @Autowired
    lateinit var update: OrderBookDecide<OrderBookUpdateCommand, OrderBookUpdatedEvent>

    @Autowired
    lateinit var publish: OrderBookDecide<OrderBookPublishCommand, OrderBookPublishedEvent>

    @Autowired
    lateinit var close: OrderBookDecide<OrderBookCloseCommand, OrderBookClosedEvent>

    @Autowired
    lateinit var eventStore: EventRepository<OrderBookEvent, OrderBookId>

    @Autowired
    lateinit var builder: Loader<OrderBookEvent, OrderBook, OrderBookId>

    @Autowired
    lateinit var orderBookDeciderImpl: OrderBookDeciderImpl

    @Test
    suspend fun `should create order book`() {
        val event = orderBookDeciderImpl.orderBookCreateDecider().invoke(OrderBookCreateCommand("TheNewOrderBook"))
        val _ = orderBookDeciderImpl.orderBookUpdateDecider()
            .invoke(OrderBookUpdateCommand(id = event.id, name = "TheNewOrderBook2"))
        val _ = orderBookDeciderImpl.orderBookPublishDecider().invoke(OrderBookPublishCommand(id = event.id))
        val _ = orderBookDeciderImpl.orderBookCloseDecider().invoke(OrderBookCloseCommand(id = event.id))
        val events = eventStore.load(event.id).toList()
        Assertions.assertThat(events.toList()).hasSize(4)
    }

    @Test
    suspend fun `should replay event to build entity`() {
        val event = create(OrderBookCreateCommand("TheNewOrderBook"))
        val _ = update(OrderBookUpdateCommand(id = event.id, name = "TheNewOrderBook2"))
        val _ = publish(OrderBookPublishCommand(id = event.id))
        val _ = close(OrderBookCloseCommand(id = event.id))
        val events = eventStore.load(event.id)
        val entity = builder.load(events)
        Assertions.assertThat(entity?.name).isEqualTo("TheNewOrderBook2")
        Assertions.assertThat(entity?.status).isEqualTo(OrderBookState.Closed)
    }

    @Test
    suspend fun `should replay all events to rebuild entities`() {
        val event = create(OrderBookCreateCommand("TheNewOrderBook"))
        val _ = update(OrderBookUpdateCommand(id = event.id, name = "TheNewOrderBook2"))
        val _ = publish(OrderBookPublishCommand(id = event.id))
        val _ = close(OrderBookCloseCommand(id = event.id))

        val _ = create(OrderBookCreateCommand("AndAnotherOrderBook"))

        orderBookDeciderImpl.replayHistory()
    }
}
