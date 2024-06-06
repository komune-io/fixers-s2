package s2.spring.sourcing.data.mongodb

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import s2.spring.sourcing.data.event.EventSourcing

@Repository
interface SpringDataEventRepository<EVENT, ID>: CoroutineCrudRepository<EventSourcing<ID>, String> {
	suspend fun findAllByObjId(objId: ID) : Flow<EventSourcing<ID>>
}
