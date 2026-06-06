package s2.automate.core.persist

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.error.ERROR_ENTITY_NOT_FOUND
import s2.automate.core.error.ERROR_PERSIST_LAMBDA_THROW
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
	 * Per-id load with classified failures. The default implementation is built
	 * on top of the existing [load] overload — it materialises the ids, runs the
	 * legacy load, and:
	 *  - emits [LoadOutcome.Loaded] for ids whose entity came back;
	 *  - emits [LoadOutcome.Rejected] (ERROR_ENTITY_NOT_FOUND) for ids the
	 *    legacy load skipped (null-emission or absent from the result flow);
	 *  - emits [LoadOutcome.Transient] (ERROR_PERSIST_LAMBDA_THROW) for every
	 *    id when the legacy load throws — the throw aborts the whole flow so
	 *    we can't distinguish per-id, but Transient lets the consumer retry.
	 *
	 * Implementations SHOULD override this when their backing store supports
	 * per-id failure classification (e.g. distinguishing "no row" from "JDBC
	 * timeout"). The engine's `evolveWithOutcomes` path consumes this method
	 * directly — overriding it is the supported way to surface rich failures
	 * without aborting sibling commands in the same batch.
	 */
	suspend fun loadWithOutcomes(
		automateContexts: AutomateContext<AUTOMATE>,
		ids: Flow<ID & Any>,
	): Flow<LoadOutcome<ID & Any, ENTITY>> = flow {
		val idList = ids.toList()
		if (idList.isEmpty()) return@flow
		val loaded = mutableMapOf<ID & Any, ENTITY>()
		val failure: Throwable? = try {
			load(automateContexts, idList.asFlow()).collect { entity ->
				entity?.s2Id()?.let { id -> loaded[id] = entity }
			}
			null
		} catch (e: CancellationException) {
			// Cooperative cancellation must propagate up the coroutine tree — do
			// NOT convert it into a Transient outcome, otherwise structured
			// concurrency unwinding stalls and the parent never sees the cancel.
			throw e
		} catch (e: Throwable) {
			e
		}
		if (failure != null) {
			idList.forEach { id ->
				emit(LoadOutcome.Transient<ID & Any, ENTITY>(id, ERROR_PERSIST_LAMBDA_THROW(failure)))
			}
			return@flow
		}
		idList.forEach { id ->
			val entity = loaded[id]
			if (entity != null) {
				emit(LoadOutcome.Loaded(id, entity))
			} else {
				emit(LoadOutcome.Rejected(id, ERROR_ENTITY_NOT_FOUND(id.toString())))
			}
		}
	}

	suspend fun persistInitWithOutcomes(
		transitionContexts: Flow<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, AUTOMATE>>
	): Flow<PersistOutcome<EVENT>> = flow {
		transitionContexts.collect { ctx ->
			persistInit(flowOf(ctx)).collect { event ->
				emit(PersistOutcome.Success(msgId = ctx.msgId, event = event))
			}
		}
	}

	suspend fun persistWithOutcomes(
		transitionContexts: Flow<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, AUTOMATE>>
	): Flow<PersistOutcome<EVENT>> = flow {
		transitionContexts.collect { ctx ->
			persist(flowOf(ctx)).collect { event ->
				emit(PersistOutcome.Success(msgId = ctx.msgId, event = event))
			}
		}
	}
}
