package s2.automate.core.persist

import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.TransitionAppliedContext
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

interface AutomatePersister<STATE, ID, ENTITY, EVENT, AUTOMATE> where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID> {
	suspend fun persist(transitionContext: InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, AUTOMATE>): ENTITY
	suspend fun persist(transitionContext: TransitionAppliedContext<STATE, ID, ENTITY, EVENT, AUTOMATE>): ENTITY
	suspend fun load(automateContext: AutomateContext<AUTOMATE>, id: ID & Any): ENTITY?
}
