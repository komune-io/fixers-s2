package s2.automate.core.snap

import kotlinx.coroutines.flow.flowOf
import s2.automate.core.error.ERROR_ENTITY_NOT_FOUND
import s2.automate.core.error.asException
import s2.dsl.automate.Evt
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.sourcing.dsl.Loader
import s2.sourcing.dsl.snap.SnapRepository

class SnapPersister<STATE, ID, ENTITY, EVENT>(
    private val projectionLoader: Loader<EVENT, ENTITY, ID>,
    private val snapRepository: SnapRepository<ENTITY, ID>?,
    private val retryTaskChannel: RetryTaskChannel?
) where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID>,
EVENT : Evt,
EVENT : WithS2Id<ID> {

    suspend fun persist(event: EVENT): Pair<ENTITY, EVENT> {
        return retryTaskChannel?.addToPersistQueue(event.s2Id(), event, ::persistSnap)
            ?: persistSnap(event)
    }

    private suspend fun persistSnap(event: EVENT): Pair<ENTITY, EVENT> {
        val id = event.s2Id()
        val entityMutated = projectionLoader.loadAndEvolve(id, flowOf(event))
            ?: throw ERROR_ENTITY_NOT_FOUND(event.s2Id().toString()).asException()
        val entity = snapRepository?.save(entityMutated) ?: entityMutated
        return entity to event
    }
}
