package s2.sourcing.dsl.snap

import kotlinx.coroutines.flow.Flow
import s2.dsl.automate.Evt
import s2.dsl.automate.model.WithS2Id
import s2.sourcing.dsl.Loader
import s2.sourcing.dsl.view.ViewLoader

class SnapLoader<EVENT, ENTITY, ID : Any>(
    private val snapRepository: SnapRepository<ENTITY, ID>,
    private val viewLoader: ViewLoader<EVENT, ENTITY, ID>
): Loader<EVENT, ENTITY, ID> where
EVENT: Evt,
EVENT: WithS2Id<ID>,
ENTITY: WithS2Id<ID> {

    override suspend fun load(id: ID): ENTITY? = snapRepository.get(id) ?: viewLoader.load(id)

    override suspend fun loadAndEvolve(id: ID, news: Flow<EVENT>): ENTITY? =
        viewLoader.evolve(events = news, entity = load(id))

    override suspend fun load(events: Flow<EVENT>): ENTITY? = viewLoader.load(events)

    override suspend fun evolve(events: Flow<EVENT>, entity: ENTITY?): ENTITY? =
        viewLoader.evolve(events = events, entity = entity)

    override suspend fun reloadHistory(): List<ENTITY> {
        return viewLoader.reloadHistory().map { entity ->
            // The rule reads the discarded `val _` as a Boolean property; an unnamed value has no name to prefix.
            @Suppress("BooleanPropertyNaming")
            val _ = snapRepository.remove(entity.s2Id())
            snapRepository.save(entity)
        }
    }
}
