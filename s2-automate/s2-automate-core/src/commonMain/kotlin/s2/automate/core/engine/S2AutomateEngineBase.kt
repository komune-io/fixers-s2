package s2.automate.core.engine

import f2.dsl.cqrs.Message
import f2.dsl.cqrs.envelope.Envelope
import f2.dsl.cqrs.enveloped.EnvelopedFlow
import f2.dsl.fnc.operators.chunk
import f2.dsl.fnc.operators.flattenConcurrently
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
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

abstract class S2AutomateEngineBase<STATE, ID, ENTITY, EVENT>(
    protected val automateContext: AutomateContext<S2Automate>,
    protected val guardExecutor: GuardVerifier<STATE, ID, ENTITY, EVENT, S2Automate>,
    protected val persister: AutomatePersister<STATE, ID, ENTITY, EVENT, S2Automate>,
    protected val publisher: AutomateEventPublisher<STATE, ID, ENTITY, S2Automate>
) where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID> {

    protected suspend fun <COMMAND : S2InitCommand, ENTITY_OUT : ENTITY, EVENT_OUT : EVENT>
    prepareCreationContext(
        decide: suspend (cmd: Envelope<COMMAND>) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>,
        command: Envelope<COMMAND>
    ): InitTransitionAppliedContext<STATE, ID, ENTITY_OUT, EVENT_OUT, S2Automate> {
        return try {
            val (entity, event) = decide(command)
            val initTransitionContext = initTransitionContext(command.data)
            guardExecutor.evaluateInit(initTransitionContext)
            InitTransitionAppliedContext(
                automateContext = automateContext,
                msgId = command.id,
                msg = command.data,
                event = event.data,
                entity = entity
            )
        } catch (e: Exception) {
            handleException(command, e)
        }
    }

    /**
     * Like [prepareCreationContext] but does NOT re-wrap exceptions. This lets
     * [createWithOutcomes] preserve the original exception type so that non-[AutomateException]
     * throws (decide-lambda failures) correctly map to [PersistOutcome.Indeterminate] rather
     * than [PersistOutcome.Rejected].
     */
    protected suspend fun <COMMAND : S2InitCommand, ENTITY_OUT : ENTITY, EVENT_OUT : EVENT>
    prepareCreationContextForOutcomes(
        decide: suspend (cmd: Envelope<COMMAND>) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>,
        command: Envelope<COMMAND>
    ): InitTransitionAppliedContext<STATE, ID, ENTITY_OUT, EVENT_OUT, S2Automate> {
        val (entity, event) = decide(command)
        val initTransitionContext = initTransitionContext(command.data)
        guardExecutor.evaluateInit(initTransitionContext)
        return InitTransitionAppliedContext(
            automateContext = automateContext,
            msgId = command.id,
            msg = command.data,
            event = event.data,
            entity = entity
        )
    }

    protected suspend fun <COMMAND : S2Command<ID>> loadTransitionContext(
        commands: EnvelopedFlow<COMMAND>
    ): Flow<Pair<ENTITY, TransitionContext<STATE, ID, ENTITY, S2Automate, out COMMAND>>> {
        return commands.chunk(automateContext.batch.size).map { commandsChunk ->
            val byIds = commandsChunk.associateBy { it.data.id }
            commandsChunk.asFlow().mapNotNull { it.data.id }.let { ids ->
                persister.load(automateContext, ids = ids)
            }.map { entity ->
                entity ?: throw ERROR_ENTITY_NOT_FOUND(entity?.s2Id().toString()).asException()
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

    /**
     * Loads entities for a chunk of commands in a single batched [persister.load] call.
     * Returns a list of (commandEnvelope, entity) pairs. Commands whose entity is not found
     * are represented as null entity and the caller is responsible for error handling.
     */
    protected suspend fun <COMMAND : S2Command<ID>> loadBatch(
        cmds: List<Envelope<COMMAND>>
    ): List<Pair<Envelope<COMMAND>, ENTITY?>> {
        val byIds = cmds.associateBy { it.data.id }
        val ids = cmds.asFlow().mapNotNull { it.data.id }
        val entities = mutableMapOf<Any, ENTITY?>()
        persister.load(automateContext, ids = ids).collect { entity ->
            val id = entity?.s2Id()
            if (id != null) {
                entities[id] = entity
            }
        }
        return cmds.map { cmd ->
            val id = cmd.data.id
            val entity = if (id != null) entities[id] else null
            cmd to entity
        }
    }

    protected fun sendEndDoTransitionEvent(
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

    protected fun <T> handleException(command: Envelope<out Message>, e: Exception): T {
        publisher.automateTransitionError(
            AutomateTransitionError(
                msg = command.data,
                exception = e
            )
        )
        if (e is AutomateException) {
            throw e
        } else {
            throw ERROR_UNKNOWN(e).asException()
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
}
