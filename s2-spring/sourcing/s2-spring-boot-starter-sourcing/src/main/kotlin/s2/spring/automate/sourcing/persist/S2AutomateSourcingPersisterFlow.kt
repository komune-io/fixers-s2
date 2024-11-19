package s2.spring.automate.sourcing.persist

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.persist.AutomatePersisterFlow
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.sourcing.dsl.Loader
import s2.sourcing.dsl.event.EventRepository
import s2.automate.core.storing.snap.SnapPersister

class S2AutomateSourcingPersisterFlow<STATE, ID, ENTITY, EVENT>(
    private val projectionLoader: Loader<EVENT, ENTITY, ID>,
    private val eventStore: EventRepository<EVENT, ID>,
    private val snapPersister: SnapPersister<STATE, ID, ENTITY, EVENT>,
) : AutomatePersisterFlow<STATE, ID, ENTITY, EVENT, S2Automate> where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID>,
EVENT: Evt,
EVENT: WithS2Id<ID> {

    override suspend fun load(automateContexts: AutomateContext<S2Automate>, id: ID & Any): ENTITY? {
        return load(automateContexts, flowOf(id)).firstOrNull()
    }

    override suspend fun load(automateContexts: AutomateContext<S2Automate>, ids: Flow<ID & Any>): Flow<ENTITY?> {
        // TODO Fix to do here
        return ids.map { id ->
            projectionLoader.load(id)
        }
    }

    override suspend fun persistInitFlow(
        transitionContexts: Flow<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
    ): Flow<EVENT> {
        return transitionContexts.map {
            it.event
        }.let {
            persist(it)
        }.map { it.second }
    }

    override suspend fun persistFlow(
        transitionContexts: Flow<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
    ): Flow<EVENT> {
        return transitionContexts.map {
            it.event
        }.let {
            persist(it)
        }.map { it.second }
    }

    private suspend fun persist(events: Flow<EVENT>): Flow<Pair<ENTITY, EVENT>> {
        return eventStore
            .persistFlow(events)
            .map { event ->
                snapPersister.persist(event)
            }
    }

}
