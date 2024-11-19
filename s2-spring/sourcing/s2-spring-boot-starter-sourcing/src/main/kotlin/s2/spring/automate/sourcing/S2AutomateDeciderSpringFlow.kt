package s2.spring.automate.sourcing

import kotlinx.coroutines.flow.Flow
import s2.automate.core.appevent.publisher.AppEventPublisher
import s2.automate.core.sourcing.S2AutomateSourcingDeciderFlow
import s2.automate.core.sourcing.S2AutomateSourcingDeciderFlowImpl
import s2.automate.core.engine.S2AutomateEngine
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.sourcing.dsl.Decide
import s2.sourcing.dsl.Loader
import s2.sourcing.dsl.event.EventRepository

open class S2AutomateDeciderSpringFlow<ENTITY, STATE, EVENT, ID>
    : S2AutomateSourcingDeciderFlow<ENTITY, STATE, EVENT, ID> where
STATE : S2State,
EVENT : Evt,
EVENT : WithS2Id<ID>,
ENTITY : WithS2Id<ID>,
ENTITY : WithS2State<STATE> {

    private lateinit var engine: S2AutomateSourcingDeciderFlowImpl<STATE, ENTITY, ID, EVENT>

    internal fun withContext(
        automateExecutor: S2AutomateEngine<STATE, ENTITY, ID, EVENT>,
        publisher: AppEventPublisher,
        projectionLoader: Loader<EVENT, ENTITY, ID>,
        eventStore: EventRepository<EVENT, ID>
    ) {
        this.engine = S2AutomateSourcingDeciderFlowImpl(automateExecutor, publisher, projectionLoader, eventStore)
    }

    override suspend fun <COMMAND : S2InitCommand, EVENT_OUT : EVENT> decide(
        commands: Flow<COMMAND>,
        buildEvent: suspend (cmd: COMMAND) -> EVENT_OUT
    ): Flow<EVENT_OUT> = engine.decide(commands, buildEvent)

    override fun <EVENT_OUT : EVENT, COMMAND : S2InitCommand> decide(
        fnc: suspend (t: COMMAND) -> EVENT_OUT
    ): Decide<COMMAND, EVENT_OUT> = engine.decide(fnc)

    override fun <COMMAND : S2Command<ID>, EVENT_OUT : EVENT> decide(
        fnc: suspend (t: COMMAND, entity: ENTITY) -> EVENT_OUT
    ): Decide<COMMAND, EVENT_OUT> = engine.decide(fnc)

    override suspend fun <COMMAND : S2Command<ID>, EVENT_OUT : EVENT> decide(
        commands: Flow<COMMAND>,
        exec: suspend (COMMAND, ENTITY) -> EVENT_OUT
    ): Flow<EVENT_OUT> = engine.decide(commands, exec)


    suspend fun loadAll() = engine.loadAll()

    suspend fun load(id: ID) = engine.load(id)

    override suspend fun replayHistory() = engine.replayHistory()
}
