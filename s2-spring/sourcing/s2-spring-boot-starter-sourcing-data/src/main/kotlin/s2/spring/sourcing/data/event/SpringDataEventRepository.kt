package s2.spring.sourcing.data.event

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SpringDataEventRepository<EVENT, ID>: CoroutineCrudRepository<EventSourcing<ID>, String> {
	suspend fun findAllByObjId(objId: ID) : Flow<EventSourcing<ID>>
}
