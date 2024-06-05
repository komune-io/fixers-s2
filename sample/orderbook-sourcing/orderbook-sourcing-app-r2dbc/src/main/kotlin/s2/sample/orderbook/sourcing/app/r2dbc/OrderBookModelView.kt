package s2.sample.orderbook.sourcing.app.r2dbc

import s2.sample.subautomate.domain.OrderBookState
import s2.sample.subautomate.domain.model.OrderBook
import s2.sample.subautomate.domain.orderBook.OrderBookClosedEvent
import s2.sample.subautomate.domain.orderBook.OrderBookCreatedEvent
import s2.sample.subautomate.domain.orderBook.OrderBookEvent
import s2.sample.subautomate.domain.orderBook.OrderBookPublishedEvent
import s2.sample.subautomate.domain.orderBook.OrderBookUpdatedEvent
import s2.sourcing.dsl.view.View

class OrderBookModelView: View<OrderBookEvent, OrderBook> {

	override suspend fun evolve(event: OrderBookEvent, model: OrderBook?): OrderBook? = when(event) {
		is OrderBookCreatedEvent -> created(event)
		is OrderBookClosedEvent -> model?.closed(event)
		is OrderBookPublishedEvent -> model?.published(event)
		is OrderBookUpdatedEvent -> model?.updated(event)
	}

	private fun OrderBook.closed(event: OrderBookClosedEvent): OrderBook = copy(
		status = event.state
	)

	private fun OrderBook.published(event: OrderBookPublishedEvent): OrderBook = copy(
		status = event.state
	)

	private fun OrderBook.updated(event: OrderBookUpdatedEvent): OrderBook = copy(
		name = event.name
	)

	private fun created(event: OrderBookCreatedEvent) = OrderBook(
		id = event.id,
		name = event.name,
		status = OrderBookState.Created
	)
}
