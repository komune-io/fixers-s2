package s2.spring.automate.data.persister

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import s2.automate.core.config.S2BatchProperties
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.context.asBatch
import s2.automate.core.persist.AutomatePersister
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

// S6309: the suspend modifier is mandated by the AutomatePersister contract (published API).
@Suppress("kotlin:S6309")
class SpringDataAutomateCoroutinePersisterFlow<STATE, ID: Any, ENTITY, EVENT>(
	private val repository: CoroutineCrudRepository<ENTITY, ID>,
	private val batchParams: S2BatchProperties,
) : AutomatePersister<STATE, ID, ENTITY, EVENT, S2Automate> where
STATE : S2State,
ENTITY : Any,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID> {

	private val loader = AlignedEntityLoader<ID, ENTITY> { ids -> repository.findAllById(ids).toList() }

	override suspend fun load(automateContexts: AutomateContext<S2Automate>, ids: Flow<ID>): Flow<ENTITY?> =
		loader.load(ids)

	override suspend fun persistInit(
		transitionContexts: Flow<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
	): Flow<EVENT> = transitionContexts.saveAllBatched({ it.entity }, { it.event })

	override suspend fun persist(
		transitionContexts: Flow<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
	): Flow<EVENT> = transitionContexts.saveAllBatched({ it.entity }, { it.event })

	/** Saves each batch of entities in a single [repository]`.saveAll`, then emits the matching events in order. */
	private fun <CONTEXT> Flow<CONTEXT>.saveAllBatched(
		entityOf: (CONTEXT) -> ENTITY,
		eventOf: (CONTEXT) -> EVENT,
	): Flow<EVENT> = saveAllBatched(batchParams.asBatch(), entityOf, eventOf) { entities ->
		repository.saveAll(entities).collect()
	}

}
