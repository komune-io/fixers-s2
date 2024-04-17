package s2.sourcing.dsl.snap

import kotlinx.coroutines.flow.Flow
import s2.dsl.automate.Evt
import s2.dsl.automate.model.WithS2Id
import s2.sourcing.dsl.Loader
import s2.sourcing.dsl.view.ViewLoader

class SnapLoader<EVENT, ENTITY, ID>(
	private val snapRepository: SnapRepository<ENTITY, ID>,
	private val viewLoader: ViewLoader<EVENT, ENTITY, ID>
): Loader<EVENT, ENTITY, ID> where
EVENT: Evt,
EVENT: WithS2Id<ID>,
ENTITY: WithS2Id<ID> {

	override suspend fun load(id: ID & Any): ENTITY? {
		return snapRepository.get(id) ?: viewLoader.load(id)
	}

	override suspend fun loadAndEvolve(id: ID & Any, news: Flow<EVENT>): ENTITY? {
		return load(id).let { entity ->
			viewLoader.evolve(news, entity)
		}
	}

	override suspend fun load(events: Flow<EVENT>): ENTITY? {
		return viewLoader.load(events)
	}

	override suspend fun evolve(events: Flow<EVENT>, entity: ENTITY?): ENTITY? {
		return viewLoader.evolve(events, entity)
	}

	override suspend fun reloadHistory(): List<ENTITY> {
		return viewLoader.reloadHistory().onEach { entity ->
			snapRepository.remove(entity.s2Id())
			snapRepository.save(entity)
		}
	}
}
