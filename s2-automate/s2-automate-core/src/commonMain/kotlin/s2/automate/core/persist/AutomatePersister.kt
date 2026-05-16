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

	suspend fun persistInitWithOutcomes(
		transitionContexts: Flow<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, AUTOMATE>>
	): Flow<PersistOutcome<EVENT>> = persistInit(transitionContexts).map { event ->
		PersistOutcome.Success(commandId = "", event = event, transactionId = "", blockNumber = 0L)
	}

	suspend fun persistWithOutcomes(
		transitionContexts: Flow<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, AUTOMATE>>
	): Flow<PersistOutcome<EVENT>> = persist(transitionContexts).map { event ->
		PersistOutcome.Success(commandId = "", event = event, transactionId = "", blockNumber = 0L)
	}
}
