package s2.spring.automate.data.persister

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.data.repository.CrudRepository
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.persist.AutomatePersister
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

class SpringDataAutomatePersister<STATE, ID, ENTITY, EVENT>(
	private val repository: CrudRepository<ENTITY, ID>,
) : AutomatePersister<STATE, ID, ENTITY, EVENT, S2Automate> where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID> {

	override suspend fun persist(
		transitionContext: TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>,
	): ENTITY {
		return repository.save(transitionContext.entity)
	}

	override suspend fun load(automateContext: AutomateContext<S2Automate>, id: ID & Any): ENTITY? {
		return repository.findById(id).orElse(null)
	}

	override suspend fun persist(
		transitionContext: InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>
	): ENTITY {
		return repository.save(transitionContext.entity)
	}

	override suspend fun persistInitFlow(
		transitionContext: Flow<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
	): Flow<EVENT> {
		return transitionContext.map {
			repository.save(it.entity)
			it.event
		}
	}

	override suspend fun persistFlow(
		transitionContext: Flow<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
	): Flow<EVENT> {
		val eventsFlow: Flow<EVENT> = transitionContext.map { it.event }

		val entitiesFlow: Flow<ENTITY> = transitionContext.map { it.entity }
		repository.saveAll(entitiesFlow.toList())

		return eventsFlow
	}
}
