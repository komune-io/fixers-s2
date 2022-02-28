package s2.automate.core

import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.InitTransitionContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.context.TransitionContext
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

interface GuardExecutor<STATE, ID, ENTITY, AUTOMATE> where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID> {
	suspend fun evaluateInit(context: InitTransitionContext<AUTOMATE>)
	suspend fun evaluateTransition(context: TransitionContext<STATE, ID, ENTITY, AUTOMATE>)
	suspend fun verifyInitTransition(context: InitTransitionAppliedContext<STATE, ID, ENTITY, AUTOMATE>)
	suspend fun verifyTransition(context: TransitionAppliedContext<STATE, ID, ENTITY, AUTOMATE>)
}
