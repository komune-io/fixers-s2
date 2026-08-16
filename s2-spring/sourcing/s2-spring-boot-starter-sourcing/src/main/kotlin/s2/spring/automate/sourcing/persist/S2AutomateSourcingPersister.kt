package s2.spring.automate.sourcing.persist

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.persist.AutomatePersister
import s2.automate.core.storing.snap.SnapPersister
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.sourcing.dsl.Loader
import s2.sourcing.dsl.event.EventRepository

// S6309: the suspend modifier is mandated by the AutomatePersister contract (published API).
@Suppress("kotlin:S6309")
class S2AutomateSourcingPersister<STATE, ID, ENTITY, EVENT>(
    private val projectionLoader: Loader<EVENT, ENTITY, ID>,
    private val eventStore: EventRepository<EVENT, ID>,
    private val snapPersister: SnapPersister<STATE, ID, ENTITY, EVENT>,
) : AutomatePersister<STATE, ID, ENTITY, EVENT, S2Automate> where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID>,
EVENT: Evt,
EVENT: WithS2Id<ID> {

    override suspend fun load(automateContexts: AutomateContext<S2Automate>, ids: Flow<ID & Any>): Flow<ENTITY?> {
        return ids.map { id ->
            projectionLoader.load(id)
        }
    }

    override suspend fun persistInit(
        transitionContexts: Flow<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
    ): Flow<EVENT> = transitionContexts.map { it.event }.persistAndSnap()

    override suspend fun persist(
        transitionContexts: Flow<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
    ): Flow<EVENT> = transitionContexts.map { it.event }.persistAndSnap()

    /** Appends the events to the event store, then refreshes the snapshot of each touched entity. */
    private suspend fun Flow<EVENT>.persistAndSnap(): Flow<EVENT> {
        return eventStore
            .persist(this)
            .map { event ->
                snapPersister.persist(event).second
            }
    }

}
