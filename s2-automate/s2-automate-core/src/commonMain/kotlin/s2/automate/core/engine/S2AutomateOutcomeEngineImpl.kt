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
            // Per-command preparation within each chunk. Failures captured as pre-computed outcomes.
            val successCtxs =
                mutableListOf<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>()
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
                        onFailure = { t ->
                            @Suppress("UNCHECKED_CAST")
                            failures.add(t.toPersistOutcome<EVENT>(command.id))
                        }
                    )
            }

            val persistedOutcomes: List<PersistOutcome<EVENT>> = if (successCtxs.isNotEmpty()) {
                persistInitWithOutcomes(successCtxs.asFlow()).toList().map { it.data }
            } else {
                emptyList()
            }

            (failures + persistedOutcomes).map { outcome ->
                @Suppress("UNCHECKED_CAST")
                outcome as PersistOutcome<EVENT_OUT>
            }.asFlow()
        }.flattenConcurrently(automateContext.batch.concurrency).mapToEnvelope(type = "PersistOutcome")
    }

    // B.2: batched load per chunk; B.3: msgId-keyed correlation
    override suspend fun <COMMAND : S2Command<ID>, ENTITY_OUT : ENTITY, EVENT_OUT : EVENT> doTransitionWithOutcomes(
        commands: EnvelopedFlow<COMMAND>,
        exec: suspend (Envelope<out COMMAND>, ENTITY) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
    ): EnvelopedFlow<PersistOutcome<EVENT_OUT>> {
        return commands.chunk(automateContext.batch.size).map { chunk ->
            // B.2: single batched load for the whole chunk
            val loaded = loadBatch(chunk)

            val successCtxs =
                mutableListOf<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>()
            val failures = mutableListOf<PersistOutcome<EVENT>>()

            loaded.forEach { (commandEnvelope, entity) ->
                runCatching {
                    val e = entity
                        ?: throw ERROR_ENTITY_NOT_FOUND(commandEnvelope.data.id.toString()).asException()
                    val transitionContext = TransitionContext(
                        automateContext = automateContext,
                        from = e.s2State(),
                        command = commandEnvelope,
                        entity = e
                    )
                    publisher.automateTransitionStarted(
                        AutomateTransitionStarted(
                            from = e.s2State(),
                            msg = commandEnvelope
                        )
                    )
                    guardExecutor.evaluateTransition(transitionContext)
                    val fromState = e.s2State()
                    val (entityMutated, result) = exec(transitionContext.command, e)
                    @Suppress("UNCHECKED_CAST")
                    TransitionAppliedContext(
                        automateContext = automateContext,
                        msgId = transitionContext.command.id,
                        from = fromState,
                        msg = transitionContext.command.data,
                        event = result.data,
                        entity = entityMutated
                    ) as TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>
                }.fold(
                    onSuccess = { ctx -> successCtxs.add(ctx) },
                    onFailure = { t ->
                        @Suppress("UNCHECKED_CAST")
                        failures.add(t.toPersistOutcome<EVENT>(commandEnvelope.id))
                    }
                )
            }

            // B.3: O(1) msgId-keyed correlation map
            val persistedOutcomes: List<PersistOutcome<EVENT>> = if (successCtxs.isNotEmpty()) {
                val ctxByMsgId = successCtxs.associateBy { it.msgId }
                persistWithOutcomes(successCtxs.asFlow()).toList().also { outcomes ->
                    outcomes.forEach { outcome ->
                        if (outcome is PersistOutcome.Success) {
                            // B.3: use msgId for O(N) lookup instead of event-equality O(N²)
                            val ctx = ctxByMsgId[outcome.msgId]
                            ctx?.let {
                                sendEndDoTransitionEvent(
                                    it.entity.s2State(), it.from, it.msg, it.entity
                                )
                            }
                        }
                    }
                }
            } else {
                emptyList()
            }

            (failures + persistedOutcomes).map { outcome ->
                @Suppress("UNCHECKED_CAST")
                outcome as PersistOutcome<EVENT_OUT>
            }.asFlow()
        }.flattenConcurrently(automateContext.batch.concurrency).mapToEnvelope(type = "PersistOutcome")
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

    private fun <EVENT_OUT> Throwable.toPersistOutcome(msgId: String): PersistOutcome<EVENT_OUT> =
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
}
