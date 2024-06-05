package s2.sample.orderbook.sourcing.app.r2dbc

import com.redis.lettucemod.api.StatefulRedisModulesConnection
import com.redis.lettucemod.search.Field
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import s2.sample.orderbook.sourcing.app.r2dbc.redis.RedisIndexField
import s2.sample.orderbook.sourcing.app.r2dbc.redis.RedisSnapView
import s2.sample.subautomate.domain.model.OrderBook
import s2.sourcing.dsl.snap.SnapRepository

@Component
class OrderBookSnapView(
    searchConnection: StatefulRedisModulesConnection<String, String>,
): SnapRepository<OrderBook, String>  {

    @PostConstruct
    fun init() = runBlocking {
        redisSnapView.createIndex<OrderBook>(
            RedisIndexField(OrderBook::id.name, Field.Type.TAG)
        )
    }

    private val redisSnapView = RedisSnapView(searchConnection)

    override suspend fun get(id: String): OrderBook? {
        return redisSnapView.get<OrderBook>(id)
    }

    override suspend fun save(entity: OrderBook): OrderBook {
        return redisSnapView.save(entity.id, entity)
    }

    override suspend fun remove(id: String): Boolean {
        return redisSnapView.delete<OrderBook>(id)
    }
}

fun String?.default(default: String) = this ?: default
