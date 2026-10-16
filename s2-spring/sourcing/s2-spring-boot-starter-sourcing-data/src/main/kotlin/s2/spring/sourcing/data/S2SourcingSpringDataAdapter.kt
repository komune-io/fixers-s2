package s2.spring.sourcing.data

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.beans.factory.annotation.Autowired
import s2.dsl.automate.Evt
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.sourcing.dsl.event.EventRepository
import s2.sourcing.dsl.snap.SnapRepository
import s2.sourcing.dsl.view.View
import s2.spring.automate.sourcing.S2AutomateDeciderSpring
import s2.spring.automate.sourcing.S2AutomateDeciderSpringAdapter
import s2.spring.sourcing.data.event.EventRepositoryFactory

abstract class S2SourcingSpringDataAdapter<ENTITY, STATE, EVENT, ID, EXECUTOR>(
	executor: EXECUTOR,
	view: View<EVENT, ENTITY>,
	snapRepository: SnapRepository<ENTITY, ID>? = null
): S2AutomateDeciderSpringAdapter<ENTITY, STATE, EVENT, ID, EXECUTOR>(executor, view, snapRepository),
	SmartInitializingSingleton where
STATE: S2State,
ENTITY: WithS2State<STATE>,
ENTITY: WithS2Id<ID>,
EVENT: Evt,
EVENT: WithS2Id<ID>,
EXECUTOR : S2AutomateDeciderSpring<ENTITY, STATE, EVENT, ID> {

	@Autowired
	lateinit var eventRepositoryFactory: EventRepositoryFactory

	private val eventStore = lazy<EventRepository<EVENT, ID>> {
		LazyTableEventRepository(eventRepositoryFactory.create(entityType(), json()))
	}

	override fun eventStore(): EventRepository<EVENT, ID> = eventStore.value

	/**
	 * Creates the table on the startup thread, so it exists before the application runners
	 * (database migrations among them) touch it. A lazily initialised bean never gets here:
	 * the first operation on its store creates the table.
	 */
	override fun afterSingletonsInstantiated() {
		if (eventStore.isInitialized()) {
			runBlocking { eventStore.value.createTable() }
		}
	}


	open fun json(): Json = Json {
		ignoreUnknownKeys = true
	}
}
