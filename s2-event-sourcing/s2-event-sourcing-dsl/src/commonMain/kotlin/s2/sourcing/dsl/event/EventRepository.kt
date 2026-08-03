package s2.sourcing.dsl.event

import kotlinx.coroutines.flow.Flow
import s2.dsl.automate.Evt
import s2.dsl.automate.model.WithS2Id

interface EventRepository<EVENT, ID>
		where EVENT : Evt, EVENT : WithS2Id<ID> {
	fun load(id: ID): Flow<EVENT>
	fun loadAll(): Flow<EVENT>
	suspend fun persist(event: EVENT): EVENT
	fun persist(events: Flow<EVENT>): Flow<EVENT>
	suspend fun createTable()
}
