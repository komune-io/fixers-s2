package s2.automate.core.engine

import f2.dsl.cqrs.Message
import f2.dsl.cqrs.envelope.Envelope
import f2.dsl.cqrs.enveloped.EnvelopedFlow
import f2.dsl.fnc.operators.chunk
import f2.dsl.fnc.operators.flattenConcurrently
import f2.dsl.fnc.operators.mapToEnvelope
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
import s2.automate.core.guard.GuardVerifier
import s2.automate.core.persist.AutomatePersister
import s2.automate.core.persist.PersistOutcome
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

open class S2AutomateEngineImpl<STATE, ID, ENTITY, EVENT>(
	private val automateContext: AutomateContext<S2Automate>,
	private val guardExecutor: GuardVerifier<STATE, ID, ENTITY, EVENT, S2Automate>,
	private val persister: AutomatePersister<STATE, ID, ENTITY, EVENT, S2Automate>,
	private val publisher: AutomateEventPublisher<STATE, ID, ENTITY, S2Automate>
) : S2AutomateEngine<STATE, ENTITY, ID, EVENT> where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID> {

	override suspend fun <COMMAND : S2InitCommand, ENTITY_OUT : ENTITY, EVENT_OUT : EVENT> create(
		commands: EnvelopedFlow<COMMAND>,
		decide: suspend (cmd: Envelope<COMMAND>) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
	): EnvelopedFlow<EVENT_OUT> {
		return commands.map { command ->
			prepareCreationContext(decide, command)
		}.let { persistContext ->
			@Suppress("UNCHECKED_CAST")
			persistInit(persistContext as Flow<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>)
		}.map {
			it as Envelope<EVENT_OUT>
		}
	}

	override suspend fun <COMMAND : S2Command<ID>, ENTITY_OUT : ENTITY, EVENT_OUT : EVENT> doTransition(
		commands: EnvelopedFlow<COMMAND>,
		exec: suspend (Envelope<out COMMAND>, ENTITY) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
	): EnvelopedFlow<EVENT_OUT> {
		return loadTransitionContext(commands).map { (entity, transitionContext)->
			guardExecutor.evaluateTransition(transitionContext)
			val fromState = entity.s2State()
			val (entityMutated, result) = exec(transitionContext.command, entity)
			TransitionAppliedContext(
				automateContext = automateContext,
				from = fromState,
				msg = transitionContext.command.data,
				event = result.data,
				entity = entityMutated
			)
		}.chunk(automateContext.batch.size).map {  transitionContexts ->
			val transitionContextsFlow = transitionContexts.asFlow()
					as Flow<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
			persist(transitionContextsFlow ).map { event ->
				val context = transitionContexts.find { it.event == event }!!
				sendEndDoTransitionEvent(context.entity.s2State(), context.from, context.msg, context.entity)
				event as EVENT_OUT
			}
		}.flattenConcurrently(automateContext.batch.concurrency).mapToEnvelope(type = "Evt")
	}

	override suspend fun <COMMAND : S2InitCommand, ENTITY_OUT : ENTITY, EVENT_OUT : EVENT> createWithOutcomes(
		commands: EnvelopedFlow<COMMAND>,
		decide: suspend (cmd: Envelope<COMMAND>) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
	): EnvelopedFlow<PersistOutcome<EVENT_OUT>> {
		// Phase 1: per-command preparation. Failures are captured as pre-computed outcomes so
		// they don't abort the batch. Note: prepareCreationContextForOutcomes does NOT re-wrap
		// exceptions, so non-AutomateException (lambda throws) map to Indeterminate.
		val prepared: Flow<Either<
			InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>,
			PersistOutcome<EVENT>>> = commands.map { command ->
			runCatching { prepareCreationContextForOutcomes(decide, command) }
				.fold(
					onSuccess = { ctx ->
						@Suppress("UNCHECKED_CAST")
						val widened = ctx
							as InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>
						Either.Left(widened)
					},
					onFailure = { t ->
						@Suppress("UNCHECKED_CAST")
						Either.Right(t.toPersistOutcome<EVENT>(command.id))
					}
				)
		}

		// Phase 2: collect all preparations, then batch successful contexts through
		// persistInitWithOutcomes and merge the pre-computed failure outcomes back in.
		val failures = mutableListOf<PersistOutcome<EVENT>>()
		val successCtxs = mutableListOf<InitTransitionAppliedContext<
			STATE, ID, ENTITY, EVENT, S2Automate>>()
		prepared.toList().forEach { either ->
			when (either) {
				is Either.Left -> successCtxs.add(either.value)
				is Either.Right -> failures.add(either.value)
			}
		}
		@Suppress("UNCHECKED_CAST")
		val ctxFlow = successCtxs.asFlow()
			as Flow<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
		val persistedOutcomes: List<PersistOutcome<EVENT>> =
			if (successCtxs.isNotEmpty()) persistInitWithOutcomes(ctxFlow).toList().map { it.data }
			else emptyList()

		return (failures + persistedOutcomes).map { outcome ->
			@Suppress("UNCHECKED_CAST")
			outcome as PersistOutcome<EVENT_OUT>
		}.asFlow().mapToEnvelope(type = "PersistOutcome")
	}

	override suspend fun <COMMAND : S2Command<ID>, ENTITY_OUT : ENTITY, EVENT_OUT : EVENT> doTransitionWithOutcomes(
		commands: EnvelopedFlow<COMMAND>,
		exec: suspend (Envelope<out COMMAND>, ENTITY) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
	): EnvelopedFlow<PersistOutcome<EVENT_OUT>> {
		// Phase 1: per-command — load entity, run guard and exec. Capture any exception as a
		// pre-failure so it doesn't abort the whole batch. Successful preparations are tagged
		// Left; per-command failures are tagged Right.
		val results: Flow<Either<
			TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>,
			PersistOutcome<EVENT>>> = commands.map { command ->
			runCatching {
				val (entity, transitionContext) = loadSingleTransitionContext(command)
				guardExecutor.evaluateTransition(transitionContext)
				val fromState = entity.s2State()
				val (entityMutated, result) = exec(transitionContext.command, entity)
				@Suppress("UNCHECKED_CAST")
				TransitionAppliedContext(
					automateContext = automateContext,
					from = fromState,
					msg = transitionContext.command.data,
					event = result.data,
					entity = entityMutated
				) as TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>
			}.fold(
				onSuccess = { ctx -> Either.Left(ctx) },
				onFailure = { t ->
					@Suppress("UNCHECKED_CAST")
					Either.Right(t.toPersistOutcome<EVENT>(command.id))
				}
			)
		}

		// Phase 2: chunk successful contexts into batches and persist them together, then
		// interleave the pre-computed failure outcomes back in at their original positions.
		return results.chunk(automateContext.batch.size).map { chunk ->
			val successCtxs = chunk.filterIsInstance<Either.Left<
				TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>,
				PersistOutcome<EVENT>>>().map { it.value }
			val failures = chunk.filterIsInstance<Either.Right<
				TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>,
				PersistOutcome<EVENT>>>().map { it.value }

			val persistedOutcomes: List<PersistOutcome<EVENT>> = if (successCtxs.isNotEmpty()) {
				@Suppress("UNCHECKED_CAST")
				val ctxFlow = successCtxs.asFlow()
					as Flow<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
				persistWithOutcomes(ctxFlow).toList().also { outcomes ->
					outcomes.forEach { outcome ->
						if (outcome is PersistOutcome.Success) {
							val ctx = successCtxs.find { it.event == outcome.event }
							ctx?.let {
								sendEndDoTransitionEvent(
									it.entity.s2State(), it.from, it.msg, it.entity
								)
							}
						}
					}
				}
			} else emptyList()

			// Emit failures then successes in chunk order (original ordering within a chunk
			// is best-effort; cross-chunk order is preserved).
			(failures + persistedOutcomes).map { outcome ->
				@Suppress("UNCHECKED_CAST")
				outcome as PersistOutcome<EVENT_OUT>
			}.asFlow()
		}.flattenConcurrently(automateContext.batch.concurrency).mapToEnvelope(type = "PersistOutcome")
	}

	private suspend fun persistInit(
		contexts: Flow<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
	): EnvelopedFlow<EVENT> {
		return contexts.map {
			guardExecutor.verifyInitTransition(it)
			it
		}.let {
			persister.persistInit(it).mapToEnvelope(type = "Evt")
		}
	}

	private suspend fun persist(
		contexts: Flow<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
	): Flow<EVENT> {
		return contexts.map {
			guardExecutor.verifyTransition(it)
		}.let {
			persister.persist(it)
		}
	}

	private suspend fun persistInitWithOutcomes(
		contexts: Flow<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
	): EnvelopedFlow<PersistOutcome<EVENT>> {
		return contexts.map {
			guardExecutor.verifyInitTransition(it)
			it
		}.let {
			persister.persistInitWithOutcomes(it).mapToEnvelope(type = "PersistOutcome")
		}
	}

	private suspend fun persistWithOutcomes(
		contexts: Flow<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
	): Flow<PersistOutcome<EVENT>> {
		return contexts.map {
			guardExecutor.verifyTransition(it)
		}.let {
			persister.persistWithOutcomes(it)
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
		commands: EnvelopedFlow<COMMAND>
	): Flow<Pair<ENTITY, TransitionContext<STATE, ID, ENTITY, S2Automate, out COMMAND>>> {
		return commands.chunk(automateContext.batch.size).map { commandsChunk ->
			val byIds = commandsChunk.associateBy { it.data.id }
			commandsChunk.asFlow().mapNotNull { it.data.id }.let { ids ->
				persister.load(automateContext, ids = ids)
			}.map { entity ->
				entity?:  throw ERROR_ENTITY_NOT_FOUND(entity?.s2Id().toString()).asException()
				val command: Envelope<COMMAND> = byIds[entity.s2Id()]
					?: throw ERROR_ENTITY_NOT_FOUND(entity.s2Id().toString()).asException()
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
		}.flattenConcurrently(automateContext.batch.concurrency)
	}

	private suspend fun <COMMAND : S2Command<ID>> loadSingleTransitionContext(
		commandEnvelope: Envelope<COMMAND>
	): Pair<ENTITY, TransitionContext<STATE, ID, ENTITY, S2Automate, COMMAND>> {
		val id = commandEnvelope.data.id
			?: throw ERROR_ENTITY_NOT_FOUND("null").asException()
		val entity = persister.load(automateContext, id)
			?: throw ERROR_ENTITY_NOT_FOUND(id.toString()).asException()
		val transitionContext = TransitionContext(
			automateContext = automateContext,
			from = entity.s2State(),
			command = commandEnvelope,
			entity = entity
		)
		publisher.automateTransitionStarted(
			AutomateTransitionStarted(
				from = entity.s2State(),
				msg = commandEnvelope
			)
		)
		return entity to transitionContext
	}

	/**
	 * Like [prepareCreationContext] but does NOT re-wrap exceptions. This lets
	 * [createWithOutcomes] preserve the original exception type so that non-[AutomateException]
	 * throws (decide-lambda failures) correctly map to [PersistOutcome.Indeterminate] rather
	 * than [PersistOutcome.Rejected].
	 */
	private suspend fun <COMMAND : S2InitCommand, ENTITY_OUT : ENTITY, EVENT_OUT : EVENT>
		prepareCreationContextForOutcomes(
			decide: suspend (cmd: Envelope<COMMAND>) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>,
			command: Envelope<COMMAND>
		): InitTransitionAppliedContext<STATE, ID, ENTITY_OUT, EVENT_OUT, S2Automate> {
		val (entity, event) = decide(command)
		val initTransitionContext = initTransitionContext(command.data)
		guardExecutor.evaluateInit(initTransitionContext)
		return InitTransitionAppliedContext(
			automateContext = automateContext,
			msg = command.data,
			event = event.data,
			entity = entity
		)
	}

	private suspend fun <COMMAND : S2InitCommand, ENTITY_OUT : ENTITY, EVENT_OUT : EVENT> prepareCreationContext(
		decide: suspend (cmd: Envelope<COMMAND>) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>,
		command: Envelope<COMMAND>
	): InitTransitionAppliedContext<STATE, ID, ENTITY_OUT, EVENT_OUT, S2Automate> {
		return try {
			val (entity, event) = decide(command)
			val initTransitionContext = initTransitionContext(command.data)
			guardExecutor.evaluateInit(initTransitionContext)
			InitTransitionAppliedContext(
				automateContext = automateContext,
				msg = command.data,
				event = event.data,
				entity = entity
			)
		} catch (e: Exception) {
			handleException(command, e)
		}
	}


	private fun <T> handleException(command: Envelope<out Message>, e: Exception): T {
		publisher.automateTransitionError(
			AutomateTransitionError(
				msg = command.data,
				exception = e
			)
		)
		if(e is AutomateException) {
			throw e
		} else {
			throw ERROR_UNKNOWN(e).asException()
		}
	}

	private fun <EVENT_OUT> Throwable.toPersistOutcome(commandId: String): PersistOutcome<EVENT_OUT> {
		return if (this is AutomateException) {
			val code = errors.firstOrNull()?.type ?: "AUTOMATE_ERROR"
			PersistOutcome.Rejected(
				commandId = commandId,
				errorCode = code,
				errorMessage = message ?: code
			)
		} else {
			PersistOutcome.Indeterminate(
				commandId = commandId,
				errorCode = "LAMBDA_THROW",
				errorMessage = message ?: "Unknown error"
			)
		}
	}
}

/** Minimal Either type used locally to tag successful vs pre-failed transition contexts. */
private sealed interface Either<out L, out R> {
	data class Left<out L, out R>(val value: L) : Either<L, R>
	data class Right<out L, out R>(val value: R) : Either<L, R>
}
