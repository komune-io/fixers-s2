package s2.automate.core.engine.sourcing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import s2.automate.core.executor.S2AutomateExecutorFlow
import s2.automate.core.appevent.publisher.AppEventPublisher
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.sourcing.dsl.Decide
import s2.sourcing.dsl.Loader
import s2.sourcing.dsl.event.EventRepository

open class S2AutomateSourcingFlowEngine<STATE, ENTITY, ID, EVENT>(
	private val automateExecutor: S2AutomateExecutorFlow<STATE, ENTITY, ID, EVENT>,
	private val publisher: AppEventPublisher,
	private val projectionLoader: Loader<EVENT, ENTITY, ID>,
	private val eventStore: EventRepository<EVENT, ID>,
) : S2AutomateDeciderFlow<ENTITY, STATE, EVENT, ID> where
STATE : S2State,
EVENT : Evt,
EVENT : WithS2Id<ID>,
ENTITY : WithS2Id<ID>,
ENTITY : WithS2State<STATE> {

	override fun <EVENT_OUT : EVENT, COMMAND: S2InitCommand> decide(
		fnc: suspend (t: COMMAND) -> EVENT_OUT
	): Decide<COMMAND, EVENT_OUT> = Decide { messages ->
			decide(messages) { msg ->
				fnc(msg)
			}
		}

	override suspend fun <COMMAND: S2InitCommand, EVENT_OUT : EVENT> decide(
		commands: Flow<COMMAND>,
		buildEvent: suspend (cmd: COMMAND) -> EVENT_OUT
	): Flow<EVENT_OUT> {
		return automateExecutor.createInitFlow(commands) { cmd ->
			val event = buildEvent(cmd)
			val entity = projectionLoader.evolve(flowOf(event))!!
			entity to event
		}.also(publisher::publish)
	}

	override fun <COMMAND: S2Command<ID>, EVENT_OUT : EVENT> decide(
		fnc: suspend (t: COMMAND, entity: ENTITY) -> EVENT_OUT
	) : Decide<COMMAND, EVENT_OUT> = Decide { messages ->
		decide(messages) { command, entity ->
			fnc(command, entity)
		}
	}

	suspend fun loadAll() = eventStore.loadAll()
	suspend fun load(id: ID) = eventStore.load(id)

	override suspend fun <COMMAND: S2Command<ID>, EVENT_OUT : EVENT,> decide(
		commands: Flow<COMMAND>,
		exec: suspend (COMMAND, ENTITY) -> EVENT_OUT
	): Flow<EVENT_OUT> {
		return automateExecutor.doTransitionFlow(commands) { command, entity ->
			val event = exec(command, entity)
			val entityUpdated = projectionLoader.evolve(flowOf(event), entity)!!
			entityUpdated to event
		}.also(publisher::publish)
	}

	override suspend fun replayHistory() {
		projectionLoader.reloadHistory()
	}
}
