package s2.spring.automate.data.persister

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.asFlux
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.persist.AutomatePersister
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

class SpringDataAutomateReactivePersister<STATE, ID, ENTITY, EVENT>(
	private val repository: ReactiveCrudRepository<ENTITY, ID>,
) : AutomatePersister<STATE, ID, ENTITY, EVENT, S2Automate> where
EVENT : Evt,
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID> {

	override suspend fun persist(
		transitionContext: TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>,
	): ENTITY {
		return repository.save(transitionContext.entity).awaitSingle()
	}

	override suspend fun load(automateContext: AutomateContext<S2Automate>, id: ID & Any): ENTITY? {
		return repository.findById(id).awaitFirstOrNull()
	}

	override suspend fun persist(
		transitionContext: InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>
	): ENTITY {
		return repository.save(transitionContext.entity).awaitSingle()
	}

	override suspend fun persistInitFlow(
		transitionContext: Flow<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
	): Flow<EVENT> {
		return transitionContext.map {
			repository.save(it.entity).awaitFirstOrNull()
			it.event
		}
//		return repository.saveAll(transitionContext.map { it.entity }.asPublisher()).asFlow()
	}

	override suspend fun persistFlow(
		transitionContext: Flow<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
	): Flow<EVENT> {
		// Extract entities from the transition context
		val entitiesFlow: Flux<ENTITY> = transitionContext.map { it.entity }.asFlux()

		// Persist the entities using the repository
		val savedEntitiesFlow: Flow<ENTITY> = repository.saveAll(entitiesFlow).asFlow()

		// Map the saved entities back to their corresponding events
		return transitionContext.zip(savedEntitiesFlow) { context, _ ->
			context.event
		}
	}
}
