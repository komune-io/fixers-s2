package s2.automate.core.engine

import f2.dsl.cqrs.enveloped.EnvelopedFlow
import f2.dsl.cqrs.envelope.Envelope
import f2.dsl.fnc.operators.chunk
import f2.dsl.fnc.operators.flattenConcurrently
import f2.dsl.fnc.operators.mapToEnvelopeWithRandomId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import s2.automate.core.appevent.publisher.AutomateEventPublisher
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.guard.GuardVerifier
import s2.automate.core.persist.AutomatePersister
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

open class S2AutomateEngineImpl<STATE, ID, ENTITY, EVENT>(
    automateContext: AutomateContext<S2Automate>,
    guardExecutor: GuardVerifier<STATE, ID, ENTITY, EVENT, S2Automate>,
    persister: AutomatePersister<STATE, ID, ENTITY, EVENT, S2Automate>,
    publisher: AutomateEventPublisher<STATE, ID, ENTITY, S2Automate>
) : S2AutomateEngineBase<STATE, ID, ENTITY, EVENT>(automateContext, guardExecutor, persister, publisher),
    S2AutomateEngine<STATE, ENTITY, ID, EVENT>
where
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
        return loadTransitionContext(commands).map { (entity, transitionContext) ->
            guardExecutor.evaluateTransition(transitionContext)
            val fromState = entity.s2State()
            val (entityMutated, result) = exec(transitionContext.command, entity)
            TransitionAppliedContext(
                automateContext = automateContext,
                msgId = transitionContext.command.id,
                from = fromState,
                msg = transitionContext.command.data,
                event = result.data,
                entity = entityMutated
            )
        }.chunk(automateContext.batch.size).map { transitionContexts ->
            val transitionContextsFlow = transitionContexts.asFlow()
                as Flow<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
            persist(transitionContextsFlow).map { event ->
                val context = transitionContexts.find { it.event == event }!!
                sendEndDoTransitionEvent(context.entity.s2State(), context.from, context.msg, context.entity)
                event as EVENT_OUT
            }
        }.flattenConcurrently(automateContext.batch.concurrency).mapToEnvelopeWithRandomId(type = "Evt")
    }

    private suspend fun persistInit(
        contexts: Flow<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
    ): EnvelopedFlow<EVENT> {
        return contexts.map {
            guardExecutor.verifyInitTransition(it)
            it
        }.let {
            persister.persistInit(it).mapToEnvelopeWithRandomId(type = "Evt")
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
}
