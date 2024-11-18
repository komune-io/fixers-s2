package s2.automate.core.engine.storing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import s2.automate.core.executor.S2AutomateExecutorFlow
import s2.automate.core.appevent.publisher.AppEventPublisher
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.sourcing.dsl.Decide

open class S2AutomateStoringEngine<STATE, ENTITY, ID>(
//    private val automateExecutor: S2AutomateExecutor<STATE, ENTITY, ID, Evt>,
    private val automateExecutorFlow: S2AutomateExecutorFlow<STATE, ENTITY, ID, Evt>,
    private val publisher: AppEventPublisher
) :
    S2AutomateStoringEvolverOld<STATE, ID, ENTITY, Evt>,
    S2AutomateStoringEvolver<STATE, ID, ENTITY, Evt>
        where STATE : S2State, ENTITY : WithS2State<STATE>, ENTITY : WithS2Id<ID> {

    override suspend fun <EVENT_OUT : Evt> createWithEvent(
        command: S2InitCommand,
        buildEvent: suspend ENTITY.() -> EVENT_OUT,
        buildEntity: suspend () -> ENTITY,
    ): EVENT_OUT {
        val event = automateExecutorFlow.create(flowOf(command)) {
            val entity = buildEntity()
            val event = buildEvent(entity)
            entity to event
        }.first()
        publisher.publish(event)
        return event
    }

    override suspend fun <EVENT_OUT : Evt> createWithEvent(
        command: S2InitCommand,
        build: suspend () -> Pair<ENTITY, EVENT_OUT>,
    ): EVENT_OUT {
        val domainEvent = automateExecutorFlow.create(flowOf(command)) { _: S2InitCommand ->
            build()
        }.first()
        publisher.publish(domainEvent)
        return domainEvent
    }

    override suspend fun <EVENT_OUT : Evt> doTransition(
        command: S2Command<ID>,
        exec: suspend ENTITY.() -> Pair<ENTITY, EVENT_OUT>,
    ): EVENT_OUT {
        val event = automateExecutorFlow.doTransition(flowOf(command)) { _, entity ->
            entity.exec()
        }.first()
        publisher.publish(event)
        return event
    }

    override suspend fun <COMMAND : S2InitCommand, EVENT_OUT : Evt> evolve(
        commands: Flow<COMMAND>,
        build: suspend (cmd: COMMAND) -> Pair<ENTITY, EVENT_OUT>
    ): Flow<EVENT_OUT> {
        return automateExecutorFlow.create(commands, build).onEach { event ->
            publisher.publish(event)
        }
    }

    override suspend fun <COMMAND : S2Command<ID>, EVENT_OUT : Evt> evolve(
        commands: Flow<COMMAND>,
        exec: suspend (COMMAND, ENTITY) -> Pair<ENTITY, EVENT_OUT>
    ): Flow<EVENT_OUT> {
        return automateExecutorFlow.doTransition(commands, exec).onEach {
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

}
