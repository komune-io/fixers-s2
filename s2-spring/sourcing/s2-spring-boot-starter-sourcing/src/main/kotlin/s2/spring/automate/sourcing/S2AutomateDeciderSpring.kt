package s2.spring.automate.sourcing

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import s2.automate.core.S2AutomateExecutor
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

open class S2AutomateDeciderSpring<ENTITY, STATE, EVENT, ID> : S2AutomateDecider<ENTITY, STATE, EVENT, ID> where
STATE : S2State,
EVENT : Evt,
EVENT : WithS2Id<ID>,
ENTITY : WithS2Id<ID>,
ENTITY : WithS2State<STATE> {

	private lateinit var automateExecutor: S2AutomateExecutor<STATE, ENTITY, ID, EVENT>
	private lateinit var publisher: AppEventPublisher
	private lateinit var projectionLoader: Loader<EVENT, ENTITY, ID>
	private lateinit var eventStore: EventRepository<EVENT, ID>

	internal fun withContext(
		automateExecutor: S2AutomateExecutor<STATE, ENTITY, ID, EVENT>,
		publisher: AppEventPublisher,
		projectionLoader: Loader<EVENT, ENTITY, ID>,
		eventStore: EventRepository<EVENT, ID>
	) {
		this.automateExecutor = automateExecutor
		this.publisher = publisher
		this.projectionLoader = projectionLoader
		this.eventStore = eventStore
	}

	override suspend fun <EVENT_OUT : EVENT> init(command: S2InitCommand, buildEvent: suspend () -> EVENT_OUT): EVENT_OUT {
		return automateExecutor.create(command) {
			val event = buildEvent()
			val entity = projectionLoader.evolve(flowOf(event))!!
			entity to event
		}.second
			.also(publisher::publish)
	}

	override suspend fun <EVENT_OUT : EVENT> transition(
		command: S2Command<ID>, exec: suspend (ENTITY) -> EVENT_OUT
	): EVENT_OUT {
		return automateExecutor.doTransition(command) {
			val event = exec(this)
			val entity = projectionLoader.evolve(flowOf(event), this)!!
			entity to event
		}.second
			.also(publisher::publish)
	}

	fun <EVENT_OUT : EVENT, COMMAND: S2InitCommand> init(
		fnc: suspend (t: COMMAND) -> EVENT_OUT
	): Decide<COMMAND, EVENT_OUT> =
		Decide { msg ->
			msg.map { cmd ->
				init(cmd) {
					fnc(cmd)
				}
			}
		}

	fun <EVENT_OUT : EVENT, COMMAND: S2Command<ID>> decide(fnc: suspend (t: COMMAND, entity: ENTITY) -> EVENT_OUT)
			: Decide<COMMAND, EVENT_OUT> = Decide { msg ->
		msg.map { cmd ->
			transition(cmd) { model ->
				fnc(cmd, model)
			}
		}
	}

	suspend fun loadAll() = eventStore.loadAll()
	suspend fun load(id: ID) = eventStore.load(id)

	override suspend fun replayHistory() {
		projectionLoader.reloadHistory()
	}
}
