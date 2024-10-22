package s2.automate.core.executor

import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.InitTransitionContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.context.TransitionContext
import s2.dsl.automate.Cmd
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

interface GuardExecutor<STATE, ID, ENTITY, EVENT, AUTOMATE> where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID> {
	suspend fun evaluateInit(context: InitTransitionContext<AUTOMATE>)
	suspend fun <COMMAND: Cmd> evaluateTransition(context: TransitionContext<STATE, ID, ENTITY, AUTOMATE, COMMAND>)

	suspend fun verifyInitTransition(
		context: InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, AUTOMATE>
	): InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, AUTOMATE>

	suspend fun verifyTransition(
		context: TransitionAppliedContext<STATE, ID, ENTITY, EVENT, AUTOMATE>
	): TransitionAppliedContext<STATE, ID, ENTITY, EVENT, AUTOMATE>
}
