package s2.spring.automate.data.persister

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.persist.AutomatePersister
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

class SpringDataAutomateCoroutinePersister<STATE, ID, ENTITY>(
	private val repository: CoroutineCrudRepository<ENTITY, ID>,
) : AutomatePersister<STATE, ID, ENTITY, S2Automate> where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID> {

	override suspend fun persist(transitionContext: InitTransitionAppliedContext<STATE, ID, ENTITY, S2Automate>): ENTITY {
		return repository.save(transitionContext.entity)
	}

	override suspend fun persist(
		transitionContext: TransitionAppliedContext<STATE, ID, ENTITY, S2Automate>,
	): ENTITY {
		return repository.save(transitionContext.entity)
	}

	override suspend fun load(automateContext: AutomateContext<S2Automate>, id: ID): ENTITY? {
		return repository.findById(id)
	}

}
