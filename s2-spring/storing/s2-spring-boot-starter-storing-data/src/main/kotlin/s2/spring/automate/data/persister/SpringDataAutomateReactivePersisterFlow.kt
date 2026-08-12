package s2.spring.automate.data.persister

import f2.dsl.fnc.operators.batch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import s2.automate.core.config.S2BatchProperties
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.context.asBatch
import s2.automate.core.persist.AutomatePersister
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

// S6309: the suspend modifier is mandated by the AutomatePersister contract (published API).
@Suppress("kotlin:S6309")
class SpringDataAutomateReactivePersisterFlow<STATE, ID: Any, ENTITY, EVENT>(
	private val repository: ReactiveCrudRepository<ENTITY, ID>,
	private val batchParams: S2BatchProperties,
) : AutomatePersister<STATE, ID, ENTITY, EVENT, S2Automate> where
EVENT : Evt,
STATE : S2State,
ENTITY : Any,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID> {

	private val loader = AlignedEntityLoader<ID, ENTITY> { ids -> repository.findAllById(ids).asFlow().toList() }

	override suspend fun load(automateContexts: AutomateContext<S2Automate>, id: ID): ENTITY? = loader.load(id)

	override suspend fun load(automateContexts: AutomateContext<S2Automate>, ids: Flow<ID>): Flow<ENTITY?> =
		loader.load(ids)

	override suspend fun persistInit(
		transitionContexts: Flow<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
	): Flow<EVENT> {
		return transitionContexts.batch(batchParams.asBatch()) { contexts ->
			val entities = contexts.map { it.entity }
			val events = contexts.map { it.event }
			repository.saveAll(entities).asFlow().collect()
			events
		}
	}
	override suspend fun persist(
		transitionContexts: Flow<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
	): Flow<EVENT> {
		return transitionContexts.batch(batchParams.asBatch()) { contexts ->
			val entities = contexts.map { it.entity }
			val events = contexts.map { it.event }
			repository.saveAll(entities).asFlow().collect()
			events
		}
	}

}
