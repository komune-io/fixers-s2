package s2.spring.sourcing.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import s2.dsl.automate.Evt
import s2.dsl.automate.model.WithS2Id
import s2.sourcing.dsl.event.EventRepository

/**
 * Creates the delegate's table on the first operation instead of at construction,
 * so building the repository never blocks the calling thread. A failed creation is
 * retried by the next operation.
 */
internal class LazyTableEventRepository<EVENT, ID>(
	private val delegate: EventRepository<EVENT, ID>,
) : EventRepository<EVENT, ID> where EVENT : Evt, EVENT : WithS2Id<ID> {

	private val mutex = Mutex()

	@Volatile
	private var tableCreated = false

	override suspend fun load(id: ID): Flow<EVENT> = withTable { delegate.load(id) }

	override suspend fun loadAll(): Flow<EVENT> = withTable { delegate.loadAll() }

	override suspend fun persist(event: EVENT): EVENT = withTable { delegate.persist(event) }

	override suspend fun persist(events: Flow<EVENT>): Flow<EVENT> = withTable { delegate.persist(events) }

	override suspend fun createTable() {
		if (tableCreated) return
		mutex.withLock {
			if (!tableCreated) {
				delegate.createTable()
				tableCreated = true
			}
		}
	}

	private suspend inline fun <T> withTable(operation: () -> T): T {
		createTable()
		return operation()
	}
}
