package s2.automate.core.persist

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.TransitionAppliedContext
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

interface AutomatePersister<STATE, ID, ENTITY, EVENT, AUTOMATE> where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID> {

	suspend fun persistInit(
		transitionContexts: Flow<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, AUTOMATE>>
	): Flow<EVENT>

	suspend fun persist(
		transitionContexts: Flow<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, AUTOMATE>>
	): Flow<EVENT>

	suspend fun load(automateContexts: AutomateContext<AUTOMATE>, ids: Flow<ID & Any>): Flow<ENTITY?>

	suspend fun load(automateContexts: AutomateContext<AUTOMATE>, id: ID & Any): ENTITY?

	/**
	 * Per-item outcome variant of [persistInit]. Default wraps each emission in
	 * [PersistOutcome.Committed] with an empty `commandId`; persisters that can
	 * surface partial-failure information (e.g. ssm chaincode) override this to
	 * emit the appropriate variant per item. See `tasks/blockchain/error-management.html`.
	 */
	suspend fun persistInitWithOutcomes(
		transitionContexts: Flow<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, AUTOMATE>>
	): Flow<PersistOutcome<EVENT>> = persistInit(transitionContexts).map { event ->
		PersistOutcome.Committed(commandId = "", event = event, transactionId = "", blockNumber = 0L)
	}

	/**
	 * Per-item outcome variant of [persist]. Default wraps each emission in
	 * [PersistOutcome.Committed]; override to surface real per-item outcomes.
	 */
	suspend fun persistWithOutcomes(
		transitionContexts: Flow<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, AUTOMATE>>
	): Flow<PersistOutcome<EVENT>> = persist(transitionContexts).map { event ->
		PersistOutcome.Committed(commandId = "", event = event, transactionId = "", blockNumber = 0L)
	}
}
