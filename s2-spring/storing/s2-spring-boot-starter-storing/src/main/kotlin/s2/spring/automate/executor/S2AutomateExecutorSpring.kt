package s2.spring.automate.executor

import kotlinx.coroutines.flow.Flow
import s2.automate.core.executor.S2AutomateExecutorFlowImpl
import s2.automate.core.appevent.publisher.AppEventPublisher
import s2.automate.core.engine.storing.S2AutomateStoringEngine
import s2.automate.core.engine.storing.S2AutomateStoringEvolverOld
import s2.automate.core.engine.storing.S2AutomateStoringEvolver
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.sourcing.dsl.Decide

/**
 * A Spring-based executor for S2 Automate that supports storing and event publishing.
 *
 * @param STATE The state type of the entity.
 * @param ID The identifier type of the entity.
 * @param ENTITY The entity type.
 */
open class S2AutomateExecutorSpring<STATE, ID, ENTITY> :
    S2AutomateStoringEvolverOld<STATE, ID, ENTITY, Evt>,
    S2AutomateStoringEvolver<STATE, ID, ENTITY, Evt>
        where STATE : S2State, ENTITY : WithS2State<STATE>, ENTITY : WithS2Id<ID> {

    protected lateinit var engine: S2AutomateStoringEngine<STATE, ENTITY, ID>

    /**
     * Initializes the executor with the given context.
     *
     * @param automateExecutor The synchronous automate executor.
     * @param automateExecutorFlow The asynchronous automate executor.
     * @param publisher The event publisher.
     */
    fun withContext(
        automateExecutorFlow: S2AutomateExecutorFlowImpl<STATE, ID, ENTITY, Evt>,
        publisher: AppEventPublisher
    ) {
        this.engine = S2AutomateStoringEngine(automateExecutorFlow, publisher)
    }

    /**
     * Creates an entity with an event.
     *
     * @param command The initialization command.
     * @param buildEvent The function to build the event.
     * @param buildEntity The function to build the entity.
     * @return The created event.
     */
    override suspend fun <EVENT_OUT : Evt> createWithEvent(
        command: S2InitCommand,
        buildEvent: suspend ENTITY.() -> EVENT_OUT,
        buildEntity: suspend () -> ENTITY,
    ): EVENT_OUT = engine.createWithEvent(command, buildEvent, buildEntity)

    /**
     * Creates an entity with an event.
     *
     * @param command The initialization command.
     * @param build The function to build the entity and event.
     * @return The created event.
     */
    override suspend fun <EVENT_OUT : Evt> createWithEvent(
        command: S2InitCommand,
        build: suspend () -> Pair<ENTITY, EVENT_OUT>,
    ): EVENT_OUT = engine.createWithEvent(command, build)

    /**
     * Performs a state transition with an event.
     *
     * @param command The command to execute.
     * @param exec The function to execute the transition.
     * @return The resulting event.
     */
    override suspend fun <EVENT_OUT : Evt> doTransition(
        command: S2Command<ID>,
        exec: suspend ENTITY.() -> Pair<ENTITY, EVENT_OUT>,
    ): EVENT_OUT = engine.doTransition(command, exec)

    /**
     * Creates entities with events from a flow of commands.
     *
     * @param commands The flow of initialization commands.
     * @param build The function to build the entity and event.
     * @return The flow of created events.
     */
    override suspend fun <COMMAND : S2InitCommand, EVENT_OUT : Evt> evolve(
        commands: Flow<COMMAND>,
        build: suspend (cmd: COMMAND) -> Pair<ENTITY, EVENT_OUT>
    ): Flow<EVENT_OUT> = engine.evolve(commands, build)

    /**
     * Performs state transitions with events from a flow of commands.
     *
     * @param commands The flow of commands to execute.
     * @param exec The function to execute the transitions.
     * @return The flow of resulting events.
     */
    override suspend fun <COMMAND : S2Command<ID>, EVENT_OUT : Evt> evolve(
        commands: Flow<COMMAND>,
        exec: suspend (COMMAND, ENTITY) -> Pair<ENTITY, EVENT_OUT>
    ): Flow<EVENT_OUT> = engine.evolve(commands, exec)

    override fun <COMMAND : S2Command<ID>, EVENT_OUT : Evt> evolve(
        fnc: suspend (COMMAND, ENTITY) -> Pair<ENTITY, EVENT_OUT>
    ): Decide<COMMAND, EVENT_OUT> = engine.evolve(fnc)

    override fun <COMMAND : S2InitCommand, EVENT_OUT : Evt> evolve(
        build: suspend (cmd: COMMAND) -> Pair<ENTITY, EVENT_OUT>
    ): Decide<COMMAND, EVENT_OUT> = engine.evolve(build)

}
