package s2.automate.core.guard

import s2.automate.core.appevent.AutomateTransitionNotAccepted
import s2.automate.core.appevent.publisher.AutomateEventPublisher
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.InitTransitionContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.context.TransitionContext
import s2.automate.core.error.AutomateException
import s2.dsl.automate.Cmd
import s2.dsl.automate.Msg
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

class GuardVerifierImpl<STATE, ID, ENTITY, EVENT, AUTOMATE>(
	private val guards: List<Guard<STATE, ID, ENTITY, EVENT, AUTOMATE>>,
	private val publisher: AutomateEventPublisher<STATE, ID, ENTITY, AUTOMATE>,
): GuardVerifier<STATE, ID, ENTITY, EVENT, AUTOMATE> where
	STATE : S2State,
	ENTITY : WithS2State<STATE>,
	ENTITY : WithS2Id<ID> {

	override suspend fun evaluateInit(context: InitTransitionContext<AUTOMATE>) {
		runGuards(context.msg) { it.evaluateInit(context) }
	}

	override suspend fun <COMMAND: Cmd> evaluateTransition(
		context: TransitionContext<STATE, ID, ENTITY, AUTOMATE, COMMAND>
	) {
		runGuards(context.command.data, context.from) { it.evaluateTransition(context) }
	}

	override suspend fun verifyInitTransition(
		context: InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, AUTOMATE>
	): InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, AUTOMATE> {
		runGuards(context.msg) { it.verifyInitTransition(context) }
		return context
	}

	override suspend fun verifyTransition(
		context: TransitionAppliedContext<STATE, ID, ENTITY, EVENT, AUTOMATE>
	): TransitionAppliedContext<STATE, ID, ENTITY, EVENT, AUTOMATE> {
		runGuards(context.msg, context.from) { it.verifyTransition(context) }
		return context
	}

	/** Runs [check] against every guard, merges the results and rejects the transition on any error. */
	private suspend fun runGuards(
		msg: Msg,
		from: S2State? = null,
		check: suspend (Guard<STATE, ID, ENTITY, EVENT, AUTOMATE>) -> GuardResult,
	) {
		val result = guards.map { check(it) }.flatten()
		handleResult(result, msg, from)
	}

	private fun List<GuardResult>.flatten(): GuardResult {
		val errors = flatMap(GuardResult::errors)
		return GuardResult.error(errors)
	}

	private fun handleResult(
		result: GuardResult,
		msg: Msg,
		from: S2State? = null,
	) {
		if (result.isValid().not()) {
			publisher.automateTransitionNotAccepted(
				AutomateTransitionNotAccepted(
					from = from,
					msg = msg
				)
			)
			throw AutomateException(result.errors)
		}
	}
}
