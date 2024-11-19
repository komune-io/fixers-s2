package s2.spring.automate.sourcing

import s2.automate.core.appevent.publisher.AppEventPublisher
import s2.automate.core.sourcing.S2AutomateSourcingDecider
import s2.automate.core.sourcing.S2AutomateSourcingDeciderImpl
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

open class S2AutomateDeciderSpring<ENTITY, STATE, EVENT, ID> : S2AutomateSourcingDecider<ENTITY, STATE, EVENT, ID> where
STATE : S2State,
EVENT : Evt,
EVENT : WithS2Id<ID>,
ENTITY : WithS2Id<ID>,
ENTITY : WithS2State<STATE> {

	private lateinit var engine: S2AutomateSourcingDeciderImpl<STATE, ENTITY, ID, EVENT>

	internal fun withContext(
		automateExecutor: S2AutomateEngine<STATE, ENTITY, ID, EVENT>,
		publisher: AppEventPublisher,
		projectionLoader: Loader<EVENT, ENTITY, ID>,
		eventStore: EventRepository<EVENT, ID>
	) {
		this.engine = S2AutomateSourcingDeciderImpl(automateExecutor, publisher, projectionLoader, eventStore)
	}

	override suspend fun <EVENT_OUT : EVENT> init(command: S2InitCommand, buildEvent: suspend () -> EVENT_OUT): EVENT_OUT {
		return engine.init(command, buildEvent)
	}

	override suspend fun <EVENT_OUT : EVENT> transition(
		command: S2Command<ID>, exec: suspend (ENTITY) -> EVENT_OUT
	): EVENT_OUT {
		return engine.transition(command, exec)
	}

	fun <EVENT_OUT : EVENT, COMMAND: S2InitCommand> init(
		fnc: suspend (t: COMMAND) -> EVENT_OUT
	): Decide<COMMAND, EVENT_OUT> = engine.init(fnc)

	fun <EVENT_OUT : EVENT, COMMAND: S2Command<ID>> decide(
		fnc: suspend (t: COMMAND, entity: ENTITY) -> EVENT_OUT
	): Decide<COMMAND, EVENT_OUT> = engine.decide(fnc)

	suspend fun loadAll() = engine.loadAll()
	suspend fun load(id: ID) = engine.load(id)

	override suspend fun replayHistory() = engine.replayHistory()
}
