package s2.automate.sourcing

import kotlinx.coroutines.flow.flowOf
import s2.automate.core.appevent.AutomateInitTransitionEnded
import s2.automate.core.appevent.AutomateInitTransitionStarted
import s2.automate.core.appevent.AutomateSessionStarted
import s2.automate.core.appevent.AutomateSessionStopped
import s2.automate.core.appevent.AutomateStateEntered
import s2.automate.core.appevent.AutomateStateExited
import s2.automate.core.appevent.AutomateTransitionEnded
import s2.automate.core.appevent.AutomateTransitionError
import s2.automate.core.appevent.AutomateTransitionStarted
import s2.automate.core.appevent.publisher.AutomateEventPublisher
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.InitTransitionContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.context.TransitionContext
import s2.automate.core.error.AutomateException
import s2.automate.core.error.ERROR_ENTITY_NOT_FOUND
import s2.automate.core.error.asException
import s2.automate.core.guard.GuardExecutorImpl
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.sourcing.dsl.Loader
import s2.sourcing.dsl.event.EventRepository

open class AutomateSourcingExecutorImpl<STATE, ID, ENTITY, EVENT>(
	private val automateContext: AutomateContext<S2Automate>,
	private val guardExecutor: GuardExecutorImpl<STATE, ID, ENTITY, S2Automate>,
	private val publisher: AutomateEventPublisher<STATE, ID, ENTITY, S2Automate>,
	private val projectionBuilder: Loader<EVENT, ENTITY, ID>,
	private val eventStore: EventRepository<EVENT, ID>,
): AutomateSourcingExecutor<STATE, EVENT, ENTITY, ID> where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID>,
EVENT: Evt,
EVENT :  WithS2Id<ID> {

	@Suppress("ThrowsCount")
	override suspend fun <EVENT_OUT : EVENT> create(command: S2InitCommand, buildEvent: suspend () -> EVENT_OUT): EVENT_OUT {
		try {
			val event = buildEvent()
			val initTransitionContext = initTransitionContext(event)
			guardExecutor.evaluateInit(initTransitionContext)
			val entity = projectionBuilder.evolve(flowOf(event), null)
				?: throw ERROR_ENTITY_NOT_FOUND(event.s2Id().toString()).asException()
			persist(command, entity, event)
			sentEndCreateEvent(command, entity)
			return event
		} catch (e: AutomateException) {
			throw e
		} catch (e: Exception) {
			publisher.automateTransitionError(
				AutomateTransitionError(
					msg = command,
					exception = e
				)
			)
			throw e
		}
	}

	private suspend fun persist(command: S2InitCommand, entity: ENTITY, event: EVENT) {
		val initTransitionPersistContext = InitTransitionAppliedContext(
			automateContext = automateContext,
			msg = command,
			entity = entity
		)
		guardExecutor.verifyInitTransition(initTransitionPersistContext)
		eventStore.persist(event)
	}

	@Suppress("ThrowsCount")
	override suspend fun <EVENT_OUT : EVENT> doTransition(
		command: S2Command<ID>,
		exec: suspend ENTITY.() -> EVENT_OUT,
	): EVENT_OUT {
		try {
			val entity = projectionBuilder.load(command.id)
				?: throw ERROR_ENTITY_NOT_FOUND(command.id.toString()).asException()
			publisher.automateTransitionStarted(
				AutomateTransitionStarted(
					from = entity.s2State(),
					msg = command
				)
			)
			val event = exec(entity)
			val transitionContext = TransitionContext(
				automateContext = automateContext,
				from = entity.s2State(),
				msg = event,
				entity = entity,
			)
			guardExecutor.evaluateTransition(transitionContext)
			val entityMutated = projectionBuilder.loadAndEvolve(command.id, flowOf(event))
				?: throw ERROR_ENTITY_NOT_FOUND(event.s2Id().toString()).asException()
			persist(entity.s2State(), command, entityMutated, event)
			sendEndDoTransitionEvent(entityMutated.s2State(), transitionContext.from, command, transitionContext.entity)
			return event
		} catch (e: AutomateException) {
			throw e
		} catch (e: Exception) {
			publisher.automateTransitionError(
				AutomateTransitionError(
					msg = command,
					exception = e
				)
			)
			throw e
		}
	}

	private suspend fun persist(fromState: STATE, command: S2Command<ID>, entityMutated: ENTITY, event: EVENT) {
		val transitionPersistContext = TransitionAppliedContext(
			automateContext = automateContext,
			from = fromState,
			msg = command,
			entity = entityMutated
		)
		guardExecutor.verifyTransition(transitionPersistContext)
		eventStore.persist(event)
//		persister.persist(transitionPersistContext)
	}

	private fun initTransitionContext(
		command: Evt,
	): InitTransitionContext<S2Automate> {
		val initTransitionContext = InitTransitionContext(
			automateContext = automateContext,
			msg = command,
		)
		publisher.automateInitTransitionStarted(
			AutomateInitTransitionStarted(
				msg = command
			)
		)
		return initTransitionContext
	}

	private fun sentEndCreateEvent(command: S2InitCommand, entity: ENTITY) {
		publisher.automateInitTransitionEnded(
			AutomateInitTransitionEnded(
				to = entity.s2State(),
				msg = command,
				entity = entity
			)
		)
		publisher.automateSessionStarted(
			AutomateSessionStarted(
				automate = automateContext.automate
			)
		)
		publisher.automateStateEntered(
			AutomateStateEntered(
				state = entity.s2State(),
			)
		)
	}

	private fun sendEndDoTransitionEvent(
		to: STATE,
		fromState: STATE,
		command: S2Command<ID>,
		entity: ENTITY,
	) {
		publisher.automateTransitionEnded(
			AutomateTransitionEnded(
				to = to,
				from = fromState,
				msg = command,
				entity = entity
			)
		)
		if (automateContext.automate.isSameState(fromState, to)) {
			publisher.automateStateExited(
				AutomateStateExited(
					state = entity.s2State()
				)
			)
		}
		if (automateContext.automate.isFinalState(to)) {
			publisher.automateSessionStopped(
				AutomateSessionStopped(
					automate = automateContext.automate
				)
			)
		}
	}
}