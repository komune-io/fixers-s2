package s2.spring.sourcing.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import s2.dsl.automate.Evt
import s2.dsl.automate.model.WithS2Id
import s2.sourcing.dsl.event.EventRepository

/**
 * Creates the delegate's table, once, before its first operation, so that building the
 * store never blocks the calling thread. A failed creation is retried by the next operation.
 */
internal class LazyTableEventRepository<EVENT, ID : Any>(
    private val delegate: EventRepository<EVENT, ID>,
) : EventRepository<EVENT, ID> where EVENT : Evt, EVENT : WithS2Id<ID> {

    private val mutex = Mutex()

    @Volatile
    private var isTableCreated = false

    override fun load(id: ID): Flow<EVENT> = flow { emitAll(withTable { delegate.load(id) }) }

    override fun loadAll(): Flow<EVENT> = flow { emitAll(withTable { delegate.loadAll() }) }

    override suspend fun persist(event: EVENT): EVENT = withTable { delegate.persist(event) }

    override fun persist(events: Flow<EVENT>): Flow<EVENT> = flow { emitAll(withTable { delegate.persist(events) }) }

    override suspend fun createTable() {
        if (isTableCreated) return
        mutex.withLock {
            if (!isTableCreated) {
                delegate.createTable()
                isTableCreated = true
            }
        }
    }

    private suspend inline fun <T> withTable(operation: () -> T): T {
        createTable()
        return operation()
    }
}
