package s2.spring.automate.data.persister

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import s2.automate.core.config.S2BatchProperties
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

class SpringDataAutomateCoroutinePersisterFlow<STATE, ID: Any, ENTITY, EVENT>(
	private val repository: CoroutineCrudRepository<ENTITY, ID>,
	batchParams: S2BatchProperties,
) : BatchingSpringDataPersister<STATE, ID, ENTITY, EVENT>(batchParams) where
STATE : S2State,
ENTITY : Any,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID> {

	override suspend fun findAllById(ids: List<ID>): List<ENTITY> = repository.findAllById(ids).toList()

	override suspend fun saveAll(entities: List<ENTITY>) {
		repository.saveAll(entities).collect()
	}
}
