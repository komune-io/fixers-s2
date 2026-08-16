package s2.spring.automate.data.persister

import f2.dsl.fnc.operators.batch
import kotlinx.coroutines.flow.Flow
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

/**
 * Shared implementation behind the coroutine and reactive Spring Data persisters, which differ
 * only in how they reach their repository: [findAllById] and [saveAll] adapt the repository
 * flavour, everything else (aligned loading, batched saving) lives here.
 */
// S6309: the suspend modifier is mandated by the AutomatePersister contract (published API).
@Suppress("kotlin:S6309")
abstract class BatchingSpringDataPersister<STATE, ID : Any, ENTITY, EVENT>(
	private val batchParams: S2BatchProperties,
) : AutomatePersister<STATE, ID, ENTITY, EVENT, S2Automate> where
STATE : S2State,
ENTITY : Any,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID> {

	/** Fetches the entities the repository knows about, in any order. */
	protected abstract suspend fun findAllById(ids: List<ID>): List<ENTITY>

	/** Saves all [entities] in a single repository call. */
	protected abstract suspend fun saveAll(entities: List<ENTITY>)

	private val loader = AlignedEntityLoader<ID, ENTITY> { ids -> findAllById(ids) }

	override suspend fun load(automateContexts: AutomateContext<S2Automate>, ids: Flow<ID>): Flow<ENTITY?> =
		loader.load(ids)

	override suspend fun persistInit(
		transitionContexts: Flow<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
	): Flow<EVENT> = transitionContexts.saveAllBatched({ it.entity }, { it.event })

	override suspend fun persist(
		transitionContexts: Flow<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
	): Flow<EVENT> = transitionContexts.saveAllBatched({ it.entity }, { it.event })

	/** Saves each batch of entities in a single [saveAll] call, then emits the matching events in order. */
	private fun <CONTEXT> Flow<CONTEXT>.saveAllBatched(
		entityOf: (CONTEXT) -> ENTITY,
		eventOf: (CONTEXT) -> EVENT,
	): Flow<EVENT> = batch(batchParams.asBatch()) { contexts ->
		val entities = contexts.map(entityOf)
		val events = contexts.map(eventOf)
		saveAll(entities)
		events
	}
}
