package s2.automate.core

import s2.automate.core.context.TransitionContext
import s2.automate.core.error.ERROR_INVALID_TRANSITION
import s2.automate.core.guard.GuardAdapter
import s2.automate.core.guard.GuardResult
import s2.dsl.automate.Automate
import s2.dsl.automate.Cmd
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

class TransitionStateGuard<STATE, ID, ENTITY, EVENT, AUTOMATE>
	: GuardAdapter<STATE, ID, ENTITY, EVENT, AUTOMATE>() where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID>,
AUTOMATE : Automate {

	override suspend fun <COMMAND : Cmd> evaluateTransition(
		context: TransitionContext<STATE, ID, ENTITY, AUTOMATE, COMMAND>
	): GuardResult {
		val state = context.entity.s2State()
		val cmd = context.command
		val isCmdValid = context.automateContext.automate.isAvailableTransition(state, cmd)
		return if (isCmdValid) {
			GuardResult.valid()
		} else {
			GuardResult.error(
				ERROR_INVALID_TRANSITION(state.toString(), "$cmd")
			)
		}
	}

}
