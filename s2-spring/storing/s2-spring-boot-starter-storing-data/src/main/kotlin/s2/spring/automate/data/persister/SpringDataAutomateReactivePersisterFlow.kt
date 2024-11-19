package s2.spring.automate.data.persister

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.asFlux
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.persist.AutomatePersisterFlow
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

class SpringDataAutomateReactivePersisterFlow<STATE, ID, ENTITY, EVENT>(
	private val repository: ReactiveCrudRepository<ENTITY, ID>,
) : AutomatePersisterFlow<STATE, ID, ENTITY, EVENT, S2Automate> where
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

	override suspend fun persistInitFlow(
		transitionContexts: Flow<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
	): Flow<EVENT> = flow {
		val entities = mutableListOf<ENTITY>()
		val events = mutableListOf<EVENT>()

		transitionContexts.collect { context ->
			entities.add(context.entity)
			events.add(context.event)
		}

		repository.saveAll(entities)

		events.forEach { event ->
			emit(event)
		}
	}
	override suspend fun persistFlow(
		transitionContexts: Flow<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
	): Flow<EVENT> = flow {
		val entities = mutableListOf<ENTITY>()
		val events = mutableListOf<EVENT>()

		// Collect entities and events from the transition context
		transitionContexts.collect { context ->
			entities.add(context.entity)
			events.add(context.event)
		}

		// Persist the entities using the repository
		repository.saveAll(entities)

		// Emit the events
		events.forEach { event ->
			emit(event)
		}
	}

}
