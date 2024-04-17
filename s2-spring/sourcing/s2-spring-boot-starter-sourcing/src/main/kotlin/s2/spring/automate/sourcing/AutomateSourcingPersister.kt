package s2.spring.automate.sourcing

import kotlinx.coroutines.flow.flowOf
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.error.ERROR_ENTITY_NOT_FOUND
import s2.automate.core.error.asException
import s2.automate.core.persist.AutomatePersister
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.sourcing.dsl.Loader
import s2.sourcing.dsl.event.EventRepository
import s2.sourcing.dsl.snap.SnapRepository

class AutomateSourcingPersister<STATE, ID, ENTITY, EVENT>(
    private val projectionLoader: Loader<EVENT, ENTITY, ID>,
    private val eventStore: EventRepository<EVENT, ID>,
    private val snapRepository: SnapRepository<ENTITY, ID>?,
    private val automateSourcingPersisterSnapChannel: AutomateSourcingPersisterSnapChannel?
) : AutomatePersister<STATE, ID, ENTITY, EVENT, S2Automate> where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID>,
EVENT: Evt,
EVENT: WithS2Id<ID> {

    override suspend fun persist(
        transitionContext: InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>
    ): ENTITY {
        val event = transitionContext.event
        val id = transitionContext.entity.s2Id()
        return persist(id, event)
    }

    override suspend fun persist(
        transitionContext: TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>
    ): ENTITY {
        val event = transitionContext.event
        val id = transitionContext.entity.s2Id()
        return persist(id, event)
    }

    private suspend fun persist(id: ID & Any, event: EVENT): ENTITY {
        val entity = automateSourcingPersisterSnapChannel?.let { snapPersistChannel ->
            return snapPersistChannel.addToPersistQueue(id, event, ::persistSnap)
        } ?:  persistSnap(id, event)

        eventStore.persist(event)
        return entity
    }
    private suspend fun persistSnap(id: ID & Any, event: EVENT): ENTITY {
        val entityMutated = projectionLoader.loadAndEvolve(id, flowOf(event))
            ?: throw ERROR_ENTITY_NOT_FOUND(event.s2Id().toString()).asException()
        return snapRepository?.save(entityMutated) ?: entityMutated
    }

    override suspend fun load(automateContext: AutomateContext<S2Automate>, id: ID & Any): ENTITY? {
        return projectionLoader.load(id)
    }
}
