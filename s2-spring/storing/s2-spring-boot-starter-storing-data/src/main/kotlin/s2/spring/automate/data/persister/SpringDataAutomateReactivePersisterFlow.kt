package s2.spring.automate.data.persister

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import s2.automate.core.config.S2BatchProperties
import s2.dsl.automate.Evt
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

class SpringDataAutomateReactivePersisterFlow<STATE, ID: Any, ENTITY, EVENT>(
	private val repository: ReactiveCrudRepository<ENTITY, ID>,
	batchParams: S2BatchProperties,
) : BatchingSpringDataPersister<STATE, ID, ENTITY, EVENT>(batchParams) where
EVENT : Evt,
STATE : S2State,
ENTITY : Any,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID> {

	override suspend fun findAllById(ids: List<ID>): List<ENTITY> = repository.findAllById(ids).asFlow().toList()

	override suspend fun saveAll(entities: List<ENTITY>) {
		repository.saveAll(entities).asFlow().collect()
	}
}
