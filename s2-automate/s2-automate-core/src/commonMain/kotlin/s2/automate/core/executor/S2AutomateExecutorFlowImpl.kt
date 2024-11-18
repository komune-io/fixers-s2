package s2.automate.core.executor

import f2.dsl.cqrs.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import s2.automate.core.appevent.AutomateInitTransitionStarted
import s2.automate.core.appevent.AutomateSessionStopped
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
import s2.automate.core.persist.AutomatePersisterFlow
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

open class S2AutomateExecutorFlowImpl<STATE, ID, ENTITY, EVENT>(
	private val automateContext: AutomateContext<S2Automate>,
	private val guardExecutor: GuardExecutor<STATE, ID, ENTITY, EVENT, S2Automate>,
	private val persister: AutomatePersisterFlow<STATE, ID, ENTITY, EVENT, S2Automate>,
	private val publisher: AutomateEventPublisher<STATE, ID, ENTITY, S2Automate>
) : S2AutomateExecutorFlow<STATE, ENTITY, ID, EVENT> where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID> {

	override suspend fun <COMMAND : S2InitCommand, ENTITY_OUT : ENTITY, EVENT_OUT : EVENT> createInitFlow(
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

	override suspend fun <COMMAND : S2Command<ID>, ENTITY_OUT : ENTITY, EVENT_OUT : EVENT> doTransitionFlow(
		commands: Flow<COMMAND>,
		exec: suspend (COMMAND, ENTITY) -> Pair<ENTITY_OUT, EVENT_OUT>
	): Flow<EVENT_OUT> {
		val transitionContexts = mutableListOf<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>()

		loadTransitionContext(commands).map { (entity, transitionContext)->
			guardExecutor.evaluateTransition(transitionContext)
			val fromState = entity.s2State()
			val (entityMutated, result) = exec(transitionContext.command, entity)

			transitionContexts.add(
				TransitionAppliedContext(
					automateContext = automateContext,
					from = fromState,
					msg = transitionContext.command,
					event = result,
					entity = entityMutated
				)
			)
		}.collect{}

		val transitionContextFlow = transitionContexts.asFlow()

		val persistedEvents = persistFlow(transitionContextFlow)

		return persistedEvents.map { event ->
			val context = transitionContexts.find { it.event == event }!!
			sendEndDoTransitionEvent(context.entity.s2State(), context.from, context.msg, context.entity)
			event as EVENT_OUT
		}
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

	private suspend fun <COMMAND : S2Command<ID>> loadTransitionContext(
		commands: Flow<COMMAND>
	): Flow<Pair<ENTITY, TransitionContext<STATE, ID, ENTITY, S2Automate, COMMAND>>> {
		val commands = commands.toList()
		val byIds = commands.associateBy { it.id }
		return commands.asFlow().mapNotNull { it.id }.let { ids ->
			persister.load(automateContext, ids = ids)
		}.map { entity ->
			entity?:  throw ERROR_ENTITY_NOT_FOUND(entity?.s2Id().toString()).asException()
			val command = byIds[entity.s2Id()] ?: throw ERROR_ENTITY_NOT_FOUND(entity.s2Id().toString()).asException()
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
			entity to transitionContext
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
