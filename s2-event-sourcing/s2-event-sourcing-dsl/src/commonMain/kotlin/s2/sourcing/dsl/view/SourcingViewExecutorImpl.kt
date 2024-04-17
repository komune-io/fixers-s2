package s2.sourcing.dsl.view

import s2.dsl.automate.Evt
import s2.dsl.automate.model.WithS2Id
import s2.sourcing.dsl.snap.SnapRepository

class SourcingViewExecutorImpl< EVENT, ENTITY, ID>(
	private val evolver: View<EVENT, ENTITY>,
	private val viewBuilder: ViewLoader<EVENT, ENTITY, ID>,
	private val viewRepository: SnapRepository<ENTITY, ID>
): SourcingViewExecutor where
EVENT: Evt,
EVENT: WithS2Id<ID> {

	suspend fun evolve(id: ID & Any, msg: EVENT){
		val entity = viewRepository.get(id) ?: viewBuilder.load(id)
		evolver.evolve(msg, entity)
	}
}
