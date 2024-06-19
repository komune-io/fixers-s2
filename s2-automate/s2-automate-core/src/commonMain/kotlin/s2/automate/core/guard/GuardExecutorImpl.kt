package s2.automate.core.guard

import s2.automate.core.GuardExecutor
import s2.automate.core.appevent.AutomateTransitionNotAccepted
import s2.automate.core.appevent.publisher.AutomateEventPublisher
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.InitTransitionContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.context.TransitionContext
import s2.automate.core.error.AutomateException
import s2.dsl.automate.Msg
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

class GuardExecutorImpl<STATE, ID, ENTITY, EVENT, AUTOMATE>(
	private val guards: List<Guard<STATE, ID, ENTITY, EVENT, AUTOMATE>>,
	private val publisher: AutomateEventPublisher<STATE, ID, ENTITY, AUTOMATE>,
): GuardExecutor<STATE, ID, ENTITY, EVENT, AUTOMATE> where
	STATE : S2State,
	ENTITY : WithS2State<STATE>,
	ENTITY : WithS2Id<ID> {

	override suspend fun evaluateInit(context: InitTransitionContext<AUTOMATE>) {
		val result = guards.map { it.evaluateInit(context) }.flatten()
		handleResult(result, context.msg)
	}

	override suspend fun evaluateTransition(context: TransitionContext<STATE, ID, ENTITY, AUTOMATE>) {
		val result = guards.map { it.evaluateTransition(context) }.flatten()
		handleResult(result, context.command, context.from)
	}

	private fun List<GuardResult>.flatten(): GuardResult {
		val errors = flatMap { it.errors }
		return GuardResult.error(errors.toList())
	}

	override suspend fun verifyInitTransition(
		context: InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, AUTOMATE>
	): InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, AUTOMATE> {
		val result = guards.map { it.verifyInitTransition(context) }.flatten()
		handleResult(result, context.msg)
		return context
	}

	override suspend fun verifyTransition(
		context: TransitionAppliedContext<STATE, ID, ENTITY, EVENT, AUTOMATE>
	): TransitionAppliedContext<STATE, ID, ENTITY, EVENT, AUTOMATE> {
		val result = guards.map { it.verifyTransition(context) }.flatten()
		handleResult(result, context.msg, context.from)
		return context
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
