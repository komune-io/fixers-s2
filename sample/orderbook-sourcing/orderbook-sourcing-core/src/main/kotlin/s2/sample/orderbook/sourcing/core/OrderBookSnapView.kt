package s2.sample.orderbook.sourcing.core

import io.lettuce.core.api.StatefulRedisConnection
import org.springframework.stereotype.Component
import s2.sample.subautomate.domain.model.OrderBook
import s2.spring.sourcing.snap.redis.IndexSettings
import s2.spring.sourcing.snap.redis.RedisFieldType
import s2.spring.sourcing.snap.redis.RedisIndexField
import s2.spring.sourcing.snap.redis.RedisSnapView

@Component
class OrderBookSnapView(
    connection: StatefulRedisConnection<String, String>,
) : RedisSnapView<OrderBook>(
    connection,
    IndexSettings(phonetic = null),
    OrderBook::class,
    listOf(RedisIndexField(OrderBook::id.name, RedisFieldType.TAG)),
    OrderBook::id,
)
