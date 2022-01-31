package s2.sample.orderbook.sourcing.app.ssm

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.springframework.context.event.EventListener
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import s2.automate.core.appevent.AutomateInitTransitionEnded
import s2.automate.core.appevent.AutomateTransitionEnded
import s2.sample.subautomate.domain.model.OrderBook
import s2.sourcing.dsl.snap.SnapRepository

@Component
class RedisSnapView(
    private val template: ReactiveRedisTemplate<String, OrderBook>,
): SnapRepository<OrderBook, String>  {

    override suspend fun get(id: String): OrderBook? {
        return template.opsForSet().pop(id).awaitFirstOrNull()
    }

    override suspend fun save(entity: OrderBook): OrderBook {
        return template.opsForSet().add(entity.id, entity).asFlow().toList().let {
            entity
        }
    }
}
