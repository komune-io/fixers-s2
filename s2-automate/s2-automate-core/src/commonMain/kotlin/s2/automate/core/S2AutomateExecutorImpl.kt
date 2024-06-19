package s2.automate.core

import f2.dsl.cqrs.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
import s2.automate.core.error.ERROR_UNKNOWN
import s2.automate.core.error.asException
import s2.automate.core.guard.GuardExecutorImpl
import s2.automate.core.persist.AutomatePersister
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

open class S2AutomateExecutorImpl<STATE, ID, ENTITY, EVENT>(
	private val automateContext: AutomateContext<S2Automate>,
	private val guardExecutor: GuardExecutorImpl<STATE, ID, ENTITY, EVENT, S2Automate>,
	private val persister: AutomatePersister<STATE, ID, ENTITY, EVENT, S2Automate>,
	private val publisher: AutomateEventPublisher<STATE, ID, ENTITY, S2Automate>
) : S2AutomateExecutor<STATE, ENTITY, ID, EVENT> where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID> {

	override suspend fun <ENTITY_OUT : ENTITY, EVENT_OUT : EVENT> create(
		command: S2InitCommand,
		decide: suspend () -> Pair<ENTITY_OUT, EVENT_OUT>
	): Pair<ENTITY_OUT, EVENT_OUT> {
		return try {
			val (entity, event) = decide()
			val initTransitionContext = initTransitionContext(command)
			guardExecutor.evaluateInit(initTransitionContext)
			persist(command, entity, event)
			sendEndCreateEvent(command, entity)
			entity to event
		} catch (e: Exception) {
			handleException(command, e)
		}
	}

	private suspend fun persist(command: S2InitCommand, entity: ENTITY, event: EVENT) {
		val initTransitionPersistContext = InitTransitionAppliedContext(
			automateContext = automateContext,
			msg = command,
			event = event,
			entity = entity
		)
		persistInitFlow(flowOf(initTransitionPersistContext)).collect()
	}

	private suspend fun persistInitFlow(
		contexts: Flow<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
	): Flow<EVENT> {
		return contexts.map {
			guardExecutor.verifyInitTransition(it)
			it
		}.let {
			persister.persistInitFlow(it)
		}
	}

	override suspend fun <ENTITY_OUT : ENTITY, EVENT_OUT : EVENT> doTransition(
		command: S2Command<ID>,
		exec: suspend ENTITY.() -> Pair<ENTITY_OUT, EVENT_OUT>
	): Pair<ENTITY_OUT, EVENT_OUT> {
		return try {
			val (entity, transitionContext) = loadTransitionContext(command)
			guardExecutor.evaluateTransition(transitionContext)
			val fromState = entity.s2State()
			val (entityMutated, result) = exec(entity)
			persist(fromState, command, entityMutated, result)
			sendEndDoTransitionEvent(entityMutated.s2State(), transitionContext.from, command, entity)
			entityMutated to result
		} catch (e: Exception) {
			handleException(command, e)
		}
	}

	private suspend fun persist(fromState: STATE, command: S2Command<ID>, entityMutated: ENTITY, event: EVENT) {
		val transitionPersistContext = TransitionAppliedContext(
			automateContext = automateContext,
			from = fromState,
			msg = command,
			event = event,
			entity = entityMutated
		)
		guardExecutor.verifyTransition(transitionPersistContext)
		persister.persist(transitionPersistContext)
	}

	private suspend fun persistFlow(
		contexts: Flow<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
	): Flow<EVENT> {
		return contexts.map {
			guardExecutor.verifyTransition(it)
		}.let {
			persister.persistFlow(it)
		}
	}

	private fun initTransitionContext(
		command: Message
	): InitTransitionContext<S2Automate> {
		return InitTransitionContext(
			automateContext = automateContext,
			msg = command
		).apply {
			publisher.automateInitTransitionStarted(
				AutomateInitTransitionStarted(msg = command)
			)
		}
	}

	private fun sendEndCreateEvent(command: S2InitCommand, entity: ENTITY) {
		with(publisher) {
			automateInitTransitionEnded(
				AutomateInitTransitionEnded(
					to = entity.s2State(),
					msg = command,
					entity = entity
				)
			)
			automateSessionStarted(
				AutomateSessionStarted(automate = automateContext.automate)
			)
			automateStateEntered(
				AutomateStateEntered(state = entity.s2State())
			)
		}
	}

	private fun sendEndDoTransitionEvent(
		to: STATE,
		fromState: STATE,
		command: S2Command<ID>,
		entity: ENTITY
	) {
		with(publisher) {
			automateTransitionEnded(
				AutomateTransitionEnded(
					to = to,
					from = fromState,
					msg = command,
					entity = entity
				)
			)
			if (automateContext.automate.isSameState(fromState, to)) {
				automateStateExited(
					AutomateStateExited(state = entity.s2State())
				)
			}
			if (automateContext.automate.isFinalState(to)) {
				automateSessionStopped(
					AutomateSessionStopped(automate = automateContext.automate)
				)
			}
		}
	}

	private suspend fun loadTransitionContext(
		command: S2Command<ID>
	): Pair<ENTITY, TransitionContext<STATE, ID, ENTITY, S2Automate>> {
		val entity = persister.load(automateContext, id = command.id)
			?: throw ERROR_ENTITY_NOT_FOUND(command.id.toString()).asException()
		val transitionContext = TransitionContext(
			automateContext = automateContext,
			from = entity.s2State(),
			command = command,
			entity = entity
		)
		publisher.automateTransitionStarted(
			AutomateTransitionStarted(
				from = entity.s2State(),
				msg = command
			)
		)
		return entity to transitionContext
	}

	override suspend fun <COMMAND : S2InitCommand, ENTITY_OUT : ENTITY, EVENT_OUT : EVENT> createInit(
		commands: Flow<COMMAND>,
		decide: suspend (cmd: COMMAND) -> Pair<ENTITY_OUT, EVENT_OUT>
	): Flow<EVENT_OUT> {
		return commands.map { command ->
			prepareCreationContext(decide, command)
		}.let { persistContext ->
			@Suppress("UNCHECKED_CAST")
			persistInitFlow(persistContext as Flow<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>)
		}.map {
			it as EVENT_OUT
		}
	}

	private suspend fun <COMMAND : S2InitCommand, ENTITY_OUT : ENTITY, EVENT_OUT : EVENT> prepareCreationContext(
		decide: suspend (cmd: COMMAND) -> Pair<ENTITY_OUT, EVENT_OUT>,
		command: COMMAND
	): InitTransitionAppliedContext<STATE, ID, ENTITY_OUT, EVENT_OUT, S2Automate> {
		return try {
			val (entity, event) = decide(command)
			val initTransitionContext = initTransitionContext(command)
			guardExecutor.evaluateInit(initTransitionContext)
			InitTransitionAppliedContext(
				automateContext = automateContext,
				msg = command,
				event = event,
				entity = entity
			)
		} catch (e: Exception) {
			handleException(command, e)
		}
	}

	override suspend fun <COMMAND : S2Command<ID>, ENTITY_OUT : ENTITY, EVENT_OUT : EVENT> doTransitionFlow(
		commands: Flow<COMMAND>,
		exec: suspend (COMMAND, ENTITY) -> Pair<ENTITY_OUT, EVENT_OUT>
	): Flow<EVENT_OUT> {
		val transitionContexts = mutableListOf<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>()

		commands.collect { command ->
			val (entity, transitionContext) = loadTransitionContext(command)
			guardExecutor.evaluateTransition(transitionContext)
			val fromState = entity.s2State()
			val (entityMutated, result) = exec(command, entity)

			transitionContexts.add(
				TransitionAppliedContext(
					automateContext = automateContext,
					from = fromState,
					msg = command,
					event = result,
					entity = entityMutated
				)
			)
		}

		val transitionContextFlow = transitionContexts.asFlow()

		val persistedEvents = persistFlow(transitionContextFlow)

		return persistedEvents.map { event ->
			val context = transitionContexts.find { it.event == event }!!
			sendEndDoTransitionEvent(context.entity.s2State(), context.from, context.msg, context.entity)
			event as EVENT_OUT
		}
	}

	private fun <T> handleException(command: Message, e: Exception): T {
		publisher.automateTransitionError(
			AutomateTransitionError(
				msg = command,
				exception = e
			)
		)
		if(e is AutomateException) {
			throw e
		} else {
			throw ERROR_UNKNOWN(e).asException()
		}
	}
}
