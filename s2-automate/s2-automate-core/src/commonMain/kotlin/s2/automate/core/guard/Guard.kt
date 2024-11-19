package s2.automate.core.guard

import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.InitTransitionContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.context.TransitionContext
import s2.dsl.automate.Cmd
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

interface Guard<STATE, ID, ENTITY, EVENT, AUTOMATE> where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID> {

	/**
	 * Evaluate a guard condition before init transition.
	 *
	 * @param context the state context init
	 * @return true, if guard evaluation is successful, false otherwise.
	 */
	suspend fun evaluateInit(context: InitTransitionContext<AUTOMATE>): GuardResult

	/**
	 * Evaluate a guard condition for transition.
	 *
	 * @param context the state context transaction
	 * @return true, if guard evaluation is successful, false otherwise.
	 */
	suspend fun <COMMAND: Cmd> evaluateTransition(
		context: TransitionContext<STATE, ID, ENTITY, AUTOMATE, COMMAND>
	): GuardResult

	/**
	 * Verify a guard condition after init transition has been applied.
	 *
	 * @param context the state context init
	 * @return true, if guard evaluation is successful, false otherwise.
	 */
	suspend fun verifyInitTransition(
		context: InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, AUTOMATE>
	): GuardResult

	/**
	 * Verify a guard condition after init transition has been applied.
	 *
	 * @param context the state context init
	 * @return true, if guard evaluation is successful, false otherwise.
	 */
	suspend fun verifyTransition(context: TransitionAppliedContext<STATE, ID, ENTITY, EVENT, AUTOMATE>): GuardResult
}
