package s2.spring.automate.data.persister

import f2.dsl.fnc.operators.flattenConcurrently
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.asFlux
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.engine.BatchParams
import s2.automate.core.persist.AutomatePersister
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import ssm.chaincode.dsl.config.chunk

class SpringDataAutomateReactivePersisterFlow<STATE, ID, ENTITY, EVENT>(
	private val repository: ReactiveCrudRepository<ENTITY, ID>,
	private val batchParams: BatchParams,
) : AutomatePersister<STATE, ID, ENTITY, EVENT, S2Automate> where
EVENT : Evt,
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID> {

	override suspend fun load(automateContexts: AutomateContext<S2Automate>, id: ID & Any): ENTITY? {
		return load(automateContexts, flowOf(id)).firstOrNull()
	}

	override suspend fun load(automateContexts: AutomateContext<S2Automate>, ids: Flow<ID & Any>): Flow<ENTITY> {
		return repository.findAllById(ids.asFlux()).asFlow()
	}

	override suspend fun persistInit(
		transitionContexts: Flow<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
	): Flow<EVENT> {
		return transitionContexts.chunk(batchParams.chunk).map { contexts ->
			val entities = contexts.map { it.entity }
			val events = contexts.map { it.event }
			repository.saveAll(entities)
			events
		}.flattenConcurrently(batchParams.concurrency)
	}
	override suspend fun persist(
		transitionContexts: Flow<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
	): Flow<EVENT> {
		return transitionContexts.chunk(batchParams.chunk).map { contexts ->
			val entities = contexts.map { it.entity }
			val events = contexts.map { it.event }
			repository.saveAll(entities)
			events
		}.flattenConcurrently(batchParams.concurrency)
	}

}
