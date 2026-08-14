package s2.automate.core.guard

import s2.automate.core.context.InitTransitionContext
import s2.automate.core.error.invalidInitTransitionError
import s2.dsl.automate.Automate
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

/**
 * Rejects init commands that are not declared as an init transition of the automate.
 *
 * This is the init counterpart of [TransitionStateGuard]. It is not part of the default
 * guards of `S2SpringAdapterBase`: automates that declare their creation transition
 * without `init<...>` (for instance `transaction<CreateCmd> { to = ... }` with no `from`)
 * do not expose any init transition, and would see every creation rejected. Enable it by
 * overriding `S2SpringAdapterBase.validateInitTransitions()` or by adding it to `guards()`.
 */
class InitTransitionStateGuard<STATE, ID, ENTITY, EVENT, AUTOMATE>
	: GuardAdapter<STATE, ID, ENTITY, EVENT, AUTOMATE>() where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID>,
AUTOMATE : Automate {

	override suspend fun evaluateInit(context: InitTransitionContext<AUTOMATE>): GuardResult {
		val command = context.msg
		val isCmdValid = context.automateContext.automate.isAvailableInitTransition(command)
		return if (isCmdValid) {
			GuardResult.valid()
		} else {
			GuardResult.error(
				invalidInitTransitionError("$command")
			)
		}
	}
}
