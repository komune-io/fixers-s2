package s2.spring.automate.executor

import kotlinx.coroutines.flow.Flow
import s2.automate.core.executor.S2AutomateExecutorFlowImpl
import s2.automate.core.executor.S2AutomateExecutorImpl
import s2.automate.core.appevent.publisher.AppEventPublisher
import s2.automate.core.engine.storing.S2AutomateStoringEngine
import s2.automate.core.engine.storing.S2AutomateStoringExecutor
import s2.automate.core.engine.storing.S2AutomateStoringExecutorFlow
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

open class S2AutomateExecutorSpring<STATE, ID, ENTITY> :
    S2AutomateStoringExecutor<STATE, ID, ENTITY, Evt>,
    S2AutomateStoringExecutorFlow<STATE, ID, ENTITY, Evt>
        where STATE : S2State, ENTITY : WithS2State<STATE>, ENTITY : WithS2Id<ID> {

    protected lateinit var engine: S2AutomateStoringEngine<STATE, ENTITY, ID>

    fun withContext(
        automateExecutor: S2AutomateExecutorImpl<STATE, ID, ENTITY, Evt>,
        automateExecutorFlow: S2AutomateExecutorFlowImpl<STATE, ID, ENTITY, Evt>,
        publisher: AppEventPublisher
    ) {
        this.engine = S2AutomateStoringEngine(automateExecutor, automateExecutorFlow, publisher)
    }

    override suspend fun <EVENT_OUT : Evt> createWithEvent(
        command: S2InitCommand,
        buildEvent: suspend ENTITY.() -> EVENT_OUT,
        buildEntity: suspend () -> ENTITY,
    ): EVENT_OUT = engine.createWithEvent(command, buildEvent, buildEntity)

    override suspend fun <EVENT_OUT : Evt> createWithEvent(
        command: S2InitCommand,
        build: suspend () -> Pair<ENTITY, EVENT_OUT>,
    ): EVENT_OUT = engine.createWithEvent(command, build)

    override suspend fun <EVENT_OUT : Evt> doTransition(
        command: S2Command<ID>,
        exec: suspend ENTITY.() -> Pair<ENTITY, EVENT_OUT>,
    ): EVENT_OUT = engine.doTransition(command, exec)

    override suspend fun <COMMAND : S2InitCommand, EVENT_OUT : Evt> createWithEventFlow(
        commands: Flow<COMMAND>,
        build: suspend (cmd: COMMAND) -> Pair<ENTITY, EVENT_OUT>
    ): Flow<EVENT_OUT> = engine.createWithEventFlow(commands, build)

    override suspend fun <COMMAND : S2Command<ID>, EVENT_OUT : Evt> doTransitionFlow(
        command: Flow<COMMAND>,
        exec: suspend (COMMAND, ENTITY) -> Pair<ENTITY, EVENT_OUT>
    ): Flow<EVENT_OUT> = engine.doTransitionFlow(command, exec)

}
