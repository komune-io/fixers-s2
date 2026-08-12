package s2.spring.automate.data.persister

import f2.dsl.fnc.operators.batch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
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

	override suspend fun load(automateContexts: AutomateContext<S2Automate>, id: ID): ENTITY? {
		return load(automateContexts, flowOf(id)).firstOrNull()
	}

	/**
	 * Emits one element per requested id, in the requested order, and `null` when the
	 * repository has no entity for that id: `findAllById` on its own drops the ids it cannot
	 * find, making the corresponding commands vanish without event and without error.
	 */
	override suspend fun load(automateContexts: AutomateContext<S2Automate>, ids: Flow<ID>): Flow<ENTITY?> = flow {
		val requestedIds = ids.toList()
		if (requestedIds.isEmpty()) return@flow
		val loaded = repository.findAllById(requestedIds).toList().associateBy { it.s2Id() }
		requestedIds.forEach { id -> emit(loaded[id]) }
	}


	override suspend fun persistInit(
		transitionContexts: Flow<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
	): Flow<EVENT> {
		return transitionContexts.batch(batchParams.asBatch()) { context ->
			val entities = context.map { it.entity }
			val events = context.map { it.event }
			repository.saveAll(entities).collect()
			events
		}
	}

	override suspend fun persist(
		transitionContexts: Flow<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
	): Flow<EVENT> {
		return transitionContexts.batch(batchParams.asBatch()) { context ->
			val entities = context.map { it.entity }
			val events = context.map { it.event }
			repository.saveAll(entities).collect()
			events
		}
	}

}
