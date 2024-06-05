package s2.spring.sourcing.data

import kotlinx.serialization.json.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.core.support.ReactiveRepositoryFactorySupport
import s2.dsl.automate.Evt
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.sourcing.dsl.snap.SnapRepository
import s2.sourcing.dsl.view.View
import s2.spring.automate.sourcing.S2AutomateDeciderSpring
import s2.spring.automate.sourcing.S2AutomateDeciderSpringAdapter
import s2.spring.sourcing.data.event.EventPersisterData
import s2.spring.sourcing.data.event.SpringDataEventRepository

abstract class S2SourcingSpringDataAdapter<ENTITY, STATE, EVENT, ID, EXECUTOR>(
	executor: EXECUTOR,
	view: View<EVENT, ENTITY>,
	snapRepository: SnapRepository<ENTITY, ID>? = null
): S2AutomateDeciderSpringAdapter<ENTITY, STATE, EVENT, ID, EXECUTOR>(executor, view, snapRepository) where
STATE: S2State,
ENTITY: WithS2State<STATE>,
ENTITY: WithS2Id<ID>,
EVENT: Evt,
EVENT: WithS2Id<ID>,
EXECUTOR : S2AutomateDeciderSpring<ENTITY, STATE, EVENT, ID> {

	@Autowired
	lateinit var repositoryFactorySupport: ReactiveRepositoryFactorySupport

	override fun eventStore(): EventPersisterData<EVENT, ID> {
		val eventRepository = springDataEventRepository()
		return EventPersisterData(eventRepository, entityType()).also {
			it.json = json()
		}
	}

	open fun springDataEventRepository(): SpringDataEventRepository<EVENT, ID> {
		return repositoryFactorySupport.getRepository(SpringDataEventRepository::class.java)
				as SpringDataEventRepository<EVENT, ID>
	}

	open fun json(): Json = Json {
		ignoreUnknownKeys = true

	}
}
