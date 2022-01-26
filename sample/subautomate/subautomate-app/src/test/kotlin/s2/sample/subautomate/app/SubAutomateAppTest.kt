package s2.sample.subautomate.app

import f2.dsl.fnc.invoke
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import s2.automate.storming.event.EventPersister
import s2.automate.storming.event.StormingProjectionBuilder
import s2.sample.subautomate.app.config.SpringTestBase
import s2.sample.subautomate.domain.OrderBookState
import s2.sample.subautomate.domain.model.OrderBook
import s2.sample.subautomate.domain.model.OrderBookId
import s2.sample.subautomate.domain.orderBook.OrderBookCloseCommand
import s2.sample.subautomate.domain.orderBook.OrderBookClosedEvent
import s2.sample.subautomate.domain.orderBook.OrderBookCreateCommand
import s2.sample.subautomate.domain.orderBook.OrderBookCreatedEvent
import s2.sample.subautomate.domain.orderBook.OrderBookDecide
import s2.sample.subautomate.domain.orderBook.OrderBookEvent
import s2.sample.subautomate.domain.orderBook.OrderBookPublishCommand
import s2.sample.subautomate.domain.orderBook.OrderBookPublishedEvent
import s2.sample.subautomate.domain.orderBook.OrderBookUpdateCommand
import s2.sample.subautomate.domain.orderBook.OrderBookUpdatedEvent

internal class SubAutomateAppTest: SpringTestBase() {

	@Autowired
	lateinit var create: OrderBookDecide<OrderBookCreateCommand, OrderBookCreatedEvent>

	@Autowired
	lateinit var update: OrderBookDecide<OrderBookUpdateCommand, OrderBookUpdatedEvent>

	@Autowired
	lateinit var publish: OrderBookDecide<OrderBookPublishCommand, OrderBookPublishedEvent>

	@Autowired
	lateinit var close: OrderBookDecide<OrderBookCloseCommand, OrderBookClosedEvent>

	@Autowired
	lateinit var eventStore: EventPersister<OrderBookEvent, OrderBookId>

	@Autowired
	lateinit var builder: StormingProjectionBuilder<OrderBook, OrderBookState, OrderBookEvent, OrderBookId>

	@Autowired
	lateinit var orderBookDeciderImpl: OrderBookDeciderImpl

	@Test
	fun `Given an api`() {
		Assertions.assertThat(mongoContainer.isRunning)
	}

	@Test
	fun `should create order book`(): Unit = runBlocking {
		val event = orderBookDeciderImpl.orderBookCreateDecider().invoke(OrderBookCreateCommand("TheNewOrderBook"))
		orderBookDeciderImpl.orderBookUpdateDecider().invoke(OrderBookUpdateCommand(id = event.id, name = "TheNewOrderBook2"))
		orderBookDeciderImpl.orderBookPublishDecider().invoke(OrderBookPublishCommand(id = event.id))
		orderBookDeciderImpl.orderBookCloseDecider().invoke(OrderBookCloseCommand(id = event.id))
		val events = eventStore.load(event.id).toList()
		Assertions.assertThat(events.toList()).hasSize(4)
	}

	@Test
	fun `should replay event to build entity`(): Unit = runBlocking {
		val event = create(OrderBookCreateCommand("TheNewOrderBook"))
		update(OrderBookUpdateCommand(id = event.id, name = "TheNewOrderBook2"))
		publish(OrderBookPublishCommand(id = event.id))
		close(OrderBookCloseCommand(id = event.id))

		val events = eventStore.load(event.id)
		val entity = builder.replay(events)
		Assertions.assertThat(entity?.name).isEqualTo("TheNewOrderBook2")
		Assertions.assertThat(entity?.status).isEqualTo(OrderBookState.Closed)
	}

}