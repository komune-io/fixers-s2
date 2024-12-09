package s2.automate.core.storing

import f2.dsl.cqrs.envelope.asEnvelopeWithType
import f2.dsl.cqrs.enveloped.EnvelopedFlow
import f2.dsl.fnc.operators.mapEnvelope
import f2.dsl.fnc.operators.mapEnvelopeWithType
import f2.dsl.fnc.operators.mapToEnvelope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import s2.automate.core.appevent.publisher.AppEventPublisher
import s2.automate.core.engine.S2AutomateEngine
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.sourcing.dsl.Decide

open class S2AutomateStoringEvolverImpl<STATE, ENTITY, ID>(
    private val automateExecutor: S2AutomateEngine<STATE, ENTITY, ID, Evt>,
    private val publisher: AppEventPublisher
) :
    S2AutomateStoringEvolver<STATE, ID, ENTITY, Evt>,
    S2AutomateStoringEvolverFlow<STATE, ID, ENTITY, Evt>
        where STATE : S2State, ENTITY : WithS2State<STATE>, ENTITY : WithS2Id<ID> {

    override suspend fun <EVENT_OUT : Evt> createWithEvent(
        command: S2InitCommand,
        buildEvent: suspend ENTITY.() -> EVENT_OUT,
        buildEntity: suspend () -> ENTITY,
    ): EVENT_OUT {
        val event = automateExecutor.create(flowOf(command.asEnvelopeWithType(type = "Cmd"))) {
            val entity = buildEntity()
            val event = buildEvent(entity)
            entity to event.asEnvelopeWithType(type = "Evt")
        }.first()
        publisher.publish(event)
        return event.data
    }

    override suspend fun <EVENT_OUT : Evt> createWithEvent(
        command: S2InitCommand,
        build: suspend () -> Pair<ENTITY, EVENT_OUT>,
    ): EVENT_OUT {
        val domainEvent = automateExecutor.create(flowOf(command.asEnvelopeWithType(type ="Cmd"))) { _ ->
            val (entity, event) = build()
            entity to event.asEnvelopeWithType(type = "Evt")
        }.first()
        publisher.publish(domainEvent)
        return domainEvent.data
    }

    override suspend fun <EVENT_OUT : Evt> doTransition(
        command: S2Command<ID>,
        exec: suspend ENTITY.() -> Pair<ENTITY, EVENT_OUT>,
    ): EVENT_OUT {
        val event = automateExecutor.doTransition(flowOf(command.asEnvelopeWithType(type ="Cmd"))) { cmd, entity ->
            val (entityUpdated, event) = entity.exec()
            entityUpdated to cmd.mapEnvelopeWithType({ event }, type = "Evt")
        }.first()
        publisher.publish(event)
        return event.data
    }

    override suspend fun <COMMAND : S2InitCommand, EVENT_OUT : Evt> evolve(
        commands: Flow<COMMAND>,
        build: suspend (cmd: COMMAND) -> Pair<ENTITY, EVENT_OUT>
    ): Flow<EVENT_OUT> {
        return automateExecutor.create(commands.mapToEnvelope(type = "Cmd"),
            { cmd ->
                val (entity, event) = build(cmd.data)
                entity to cmd.mapEnvelopeWithType({event}, type = "Evt")
            }
        ).onEach { event ->
            publisher.publish(event)
        }.map { it.data }
    }

    override suspend fun <COMMAND : S2Command<ID>, EVENT_OUT : Evt> evolve(
        commands: Flow<COMMAND>,
        exec: suspend (COMMAND, ENTITY) -> Pair<ENTITY, EVENT_OUT>
    ): Flow<EVENT_OUT> {
        return automateExecutor.doTransition(commands.mapToEnvelope(type = "Cmd"),
            { cmd, entity ->
                val (entity, event) = exec(cmd.data, entity)
                entity to cmd.mapEnvelopeWithType({event}, type = "Evt")
            }
        ).map { it.data }.onEach {
            publisher.publish(it)
        }
    }

    override fun <COMMAND : S2Command<ID>, EVENT_OUT : Evt> evolve(
        fnc: suspend (COMMAND, ENTITY) -> Pair<ENTITY, EVENT_OUT>
    ): Decide<COMMAND, EVENT_OUT> = Decide { messages ->
        evolve(messages) { command, entity ->
            fnc(command, entity)
        }
    }

    override fun <COMMAND : S2InitCommand, EVENT_OUT : Evt> evolve(
        build: suspend (cmd: COMMAND) -> Pair<ENTITY, EVENT_OUT>
    ): Decide<COMMAND, EVENT_OUT> = Decide { messages ->
        evolve(messages) { command ->
            build(command)
        }
    }

    override suspend fun <COMMAND : S2InitCommand, EVENT_OUT : Evt> evolveEnvelope(
        commands: EnvelopedFlow<COMMAND>,
        build: S2EvolveInitFnc<COMMAND, ENTITY, EVENT_OUT>
    ): EnvelopedFlow<EVENT_OUT>  {
        return automateExecutor.create(commands,
            { cmd ->
                val (entity, event) = build(cmd.data)
                entity to cmd.mapEnvelopeWithType({event}, type = "Evt")
            }
        ).onEach { event ->
            publisher.publish(event)
        }
    }

    override suspend fun <COMMAND : S2Command<ID>, EVENT_OUT : Evt> evolveEnvelope(
        commands: EnvelopedFlow<COMMAND>,
        exec: S2EvolveFnc<COMMAND, ENTITY, EVENT_OUT>
    ): EnvelopedFlow<EVENT_OUT> {
        return automateExecutor.doTransition(commands,
            { cmd, entity ->
                val (updatedEntity, event) = exec(cmd.data, entity)
                updatedEntity to cmd.mapEnvelopeWithType({event}, type = "Evt")
            }
        ).onEach {
            publisher.publish(it)
        }
    }

}
