package s2.automate.core.engine

import f2.dsl.cqrs.envelope.Envelope
import f2.dsl.cqrs.enveloped.EnvelopedFlow
import f2.dsl.fnc.operators.chunk
import f2.dsl.fnc.operators.flattenConcurrently
import f2.dsl.fnc.operators.mapToEnvelope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import s2.automate.core.appevent.AutomateTransitionStarted
import s2.automate.core.appevent.publisher.AutomateEventPublisher
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.context.TransitionContext
import s2.automate.core.error.AutomateException
import s2.automate.core.error.ERROR_ENTITY_NOT_FOUND
import s2.automate.core.error.ERROR_PERSIST_LAMBDA_THROW
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

open class S2AutomateOutcomeEngineImpl<STATE, ID, ENTITY, EVENT>(
    automateContext: AutomateContext<S2Automate>,
    guardExecutor: GuardVerifier<STATE, ID, ENTITY, EVENT, S2Automate>,
    persister: AutomatePersister<STATE, ID, ENTITY, EVENT, S2Automate>,
    publisher: AutomateEventPublisher<STATE, ID, ENTITY, S2Automate>
) : S2AutomateEngineBase<STATE, ID, ENTITY, EVENT>(automateContext, guardExecutor, persister, publisher),
    S2AutomateOutcomeEngine<STATE, ENTITY, ID, EVENT>
where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID> {

    // B.1: chunk-based streaming instead of unbounded toList
    override suspend fun <COMMAND : S2InitCommand, ENTITY_OUT : ENTITY, EVENT_OUT : EVENT> createWithOutcomes(
        commands: EnvelopedFlow<COMMAND>,
        decide: suspend (cmd: Envelope<COMMAND>) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
    ): EnvelopedFlow<PersistOutcome<EVENT_OUT>> {
        return commands.chunk(automateContext.batch.size).map { chunk ->
            val (successCtxs, failures) = partitionCreations(chunk, decide)
            val persisted = persistCreationsToList(successCtxs)
            (failures + persisted).castedAsFlow<EVENT_OUT>()
        }.flattenConcurrently(automateContext.batch.concurrency).mapToEnvelope(type = "PersistOutcome")
    }

    // B.2: batched load per chunk; B.3: msgId-keyed correlation
    override suspend fun <COMMAND : S2Command<ID>, ENTITY_OUT : ENTITY, EVENT_OUT : EVENT> doTransitionWithOutcomes(
        commands: EnvelopedFlow<COMMAND>,
        exec: suspend (Envelope<out COMMAND>, ENTITY) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
    ): EnvelopedFlow<PersistOutcome<EVENT_OUT>> {
        return commands.chunk(automateContext.batch.size).map { chunk ->
            val loaded = loadBatch(chunk)
            val (successCtxs, failures) = partitionTransitions(loaded, exec)
            val persisted = persistTransitionsAndEmitEnded(successCtxs)
            (failures + persisted).castedAsFlow<EVENT_OUT>()
        }.flattenConcurrently(automateContext.batch.concurrency).mapToEnvelope(type = "PersistOutcome")
    }

    private suspend fun <COMMAND : S2InitCommand, ENTITY_OUT : ENTITY, EVENT_OUT : EVENT> partitionCreations(
        chunk: List<Envelope<COMMAND>>,
        decide: suspend (cmd: Envelope<COMMAND>) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
    ): Pair<List<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>, List<PersistOutcome<EVENT>>> {
        val successCtxs = mutableListOf<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>()
        val failures = mutableListOf<PersistOutcome<EVENT>>()
        chunk.forEach { command ->
            runCatching { prepareCreationContextForOutcomes(decide, command) }
                .fold(
                    onSuccess = { ctx ->
                        @Suppress("UNCHECKED_CAST")
                        successCtxs.add(
                            ctx as InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>
                        )
                    },
                    onFailure = { t -> failures.add(t.toPersistOutcome(command.id)) }
                )
        }
        return successCtxs to failures
    }

    private suspend fun persistCreationsToList(
        successCtxs: List<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
    ): List<PersistOutcome<EVENT>> {
        if (successCtxs.isEmpty()) return emptyList()
        return persistInitWithOutcomes(successCtxs.asFlow()).toList().map { it.data }
    }

    private suspend fun <COMMAND : S2Command<ID>, ENTITY_OUT : ENTITY, EVENT_OUT : EVENT> partitionTransitions(
        loaded: List<Pair<Envelope<COMMAND>, ENTITY?>>,
        exec: suspend (Envelope<out COMMAND>, ENTITY) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
    ): Pair<List<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>, List<PersistOutcome<EVENT>>> {
        val successCtxs = mutableListOf<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>()
        val failures = mutableListOf<PersistOutcome<EVENT>>()
        loaded.forEach { (commandEnvelope, entity) ->
            runCatching { buildAppliedContext(commandEnvelope, entity, exec) }
                .fold(
                    onSuccess = { ctx -> successCtxs.add(ctx) },
                    onFailure = { t -> failures.add(t.toPersistOutcome(commandEnvelope.id)) }
                )
        }
        return successCtxs to failures
    }

    private suspend fun <COMMAND : S2Command<ID>, ENTITY_OUT : ENTITY, EVENT_OUT : EVENT> buildAppliedContext(
        commandEnvelope: Envelope<COMMAND>,
        entity: ENTITY?,
        exec: suspend (Envelope<out COMMAND>, ENTITY) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
    ): TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate> {
        val e = entity
            ?: throw ERROR_ENTITY_NOT_FOUND(commandEnvelope.data.id.toString()).asException()
        val transitionContext = TransitionContext(
            automateContext = automateContext,
            from = e.s2State(),
            command = commandEnvelope,
            entity = e
        )
        publisher.automateTransitionStarted(
            AutomateTransitionStarted(from = e.s2State(), msg = commandEnvelope)
        )
        guardExecutor.evaluateTransition(transitionContext)
        val fromState = e.s2State()
        val (entityMutated, result) = exec(transitionContext.command, e)
        @Suppress("UNCHECKED_CAST")
        return TransitionAppliedContext(
            automateContext = automateContext,
            msgId = transitionContext.command.id,
            from = fromState,
            msg = transitionContext.command.data,
            event = result.data,
            entity = entityMutated
        ) as TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>
    }

    // B.3: msgId-keyed correlation — O(N) lookup instead of event-equality O(N²)
    private suspend fun persistTransitionsAndEmitEnded(
        successCtxs: List<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
    ): List<PersistOutcome<EVENT>> {
        if (successCtxs.isEmpty()) return emptyList()
        val ctxByMsgId = successCtxs.associateBy { it.msgId }
        return persistWithOutcomes(successCtxs.asFlow()).toList().also { outcomes ->
            outcomes.forEach { outcome ->
                if (outcome is PersistOutcome.Success) {
                    ctxByMsgId[outcome.msgId]?.let { ctx ->
                        sendEndDoTransitionEvent(ctx.entity.s2State(), ctx.from, ctx.msg, ctx.entity)
                    }
                }
            }
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

    private fun Throwable.toPersistOutcome(msgId: String): PersistOutcome<EVENT> =
        when (this) {
            is AutomateException -> PersistOutcome.Rejected(
                msgId = msgId,
                error = errors.firstOrNull() ?: ERROR_UNKNOWN(this),
            )
            else -> PersistOutcome.Indeterminate(
                msgId = msgId,
                error = ERROR_PERSIST_LAMBDA_THROW(this),
            )
        }

    @Suppress("UNCHECKED_CAST")
    private fun <EVENT_OUT> List<PersistOutcome<EVENT>>.castedAsFlow(): Flow<PersistOutcome<EVENT_OUT>> =
        (this as List<PersistOutcome<EVENT_OUT>>).asFlow()
}
