package s2.spring.automate.sourcing.persist

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.error.ERROR_ENTITY_NOT_FOUND
import s2.automate.core.error.asException
import s2.automate.core.persist.AutomatePersisterFlow
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.sourcing.dsl.Loader
import s2.sourcing.dsl.event.EventRepository
import s2.sourcing.dsl.snap.SnapRepository
import s2.spring.automate.sourcing.AutomateSourcingPersisterSnapChannel

class AutomateSourcingPersisterFlow<STATE, ID, ENTITY, EVENT>(
    private val projectionLoader: Loader<EVENT, ENTITY, ID>,
    private val eventStore: EventRepository<EVENT, ID>,
    private val snapRepository: SnapRepository<ENTITY, ID>?,
    private val automateSourcingPersisterSnapChannel: AutomateSourcingPersisterSnapChannel?
) : AutomatePersisterFlow<STATE, ID, ENTITY, EVENT, S2Automate> where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID>,
EVENT: Evt,
EVENT: WithS2Id<ID> {

    override suspend fun load(automateContext: AutomateContext<S2Automate>, ids: Flow<ID & Any>): Flow<ENTITY?> {
        // TODO Fix to do here
        return ids.map { id ->
            projectionLoader.load(id)
        }
    }


    private suspend fun persist(id: ID & Any, event: EVENT): ENTITY {
        val entity = automateSourcingPersisterSnapChannel?.let { snapPersistChannel ->
            return snapPersistChannel.addToPersistQueue(id, event, ::persistSnap).first
        } ?:  persistSnap(id, event)

        eventStore.persist(event)
        return entity.first
    }

    private suspend fun persist(events: Flow<EVENT>): Flow<Pair<ENTITY, EVENT>> {
        return  eventStore
            .persistFlow(events)
            .map { event ->
                val existingSnap: Pair<ENTITY, EVENT>? = automateSourcingPersisterSnapChannel
                    ?.let { snapPersistChannel ->
                        snapPersistChannel.addToPersistQueue(event.s2Id(), event, ::persistSnap)
                    }
                val persidtedSnap: Pair<ENTITY, EVENT> = persistSnap(event.s2Id(), event)
                existingSnap ?: persidtedSnap
            }
    }

    private suspend fun persistSnap(id: ID & Any, event: EVENT): Pair<ENTITY, EVENT> {
        val entityMutated = projectionLoader.loadAndEvolve(id, flowOf(event))
            ?: throw ERROR_ENTITY_NOT_FOUND(event.s2Id().toString()).asException()
        val entity = snapRepository?.save(entityMutated) ?: entityMutated
        return entity to event
    }

    override suspend fun persistInitFlow(
        transitionContext: Flow<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
    ): Flow<EVENT> {
        return transitionContext.map {
            it.event
        }.let {
            persist(it)
        }.map { it.second }
    }

    override suspend fun persistFlow(
        transitionContext: Flow<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, S2Automate>>
    ): Flow<EVENT> {
        return transitionContext.map {
            it.event
        }.let {
            persist(it)
        }.map { it.second }
    }
}