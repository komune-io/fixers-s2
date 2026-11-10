package s2.automate.core.persist

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.error.entityNotFoundError
import s2.automate.core.error.persistLambdaThrowError
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

interface AutomatePersister<STATE, ID : Any, ENTITY, EVENT, AUTOMATE> where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID> {

    fun persistInit(
        transitionContexts: Flow<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, AUTOMATE>>
    ): Flow<EVENT>

    /**
     * Persists every received context and emits the resulting events.
     *
     * Implementations MUST emit exactly one event per received context, in the same
     * order: the engine correlates each emitted event back to its context positionally
     * in order to publish the end-of-transition application events.
     */
    fun persist(
        transitionContexts: Flow<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, AUTOMATE>>
    ): Flow<EVENT>

    fun load(automateContexts: AutomateContext<AUTOMATE>, ids: Flow<ID>): Flow<ENTITY?>

    suspend fun load(automateContexts: AutomateContext<AUTOMATE>, id: ID): ENTITY? =
        load(automateContexts, flowOf(id)).firstOrNull()

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
    // The legacy load may fail with anything: every failure is classified Transient for each id.
    @Suppress("TooGenericExceptionCaught")
    fun loadWithOutcomes(
        automateContexts: AutomateContext<AUTOMATE>,
        ids: Flow<ID>,
    ): Flow<LoadOutcome<ID, ENTITY>> = flow {
        val idList = ids.toList()
        if (idList.isNotEmpty()) {
            val loaded = mutableMapOf<ID, ENTITY>()
            val failure: Throwable? = try {
                load(automateContexts, idList.asFlow()).collect { entity ->
                    entity?.let { loaded[it.s2Id()] = it }
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
            idList.forEach { id ->
                val entity = loaded[id]
                val outcome: LoadOutcome<ID, ENTITY> = when {
                    failure != null -> LoadOutcome.Transient(id, persistLambdaThrowError(failure))
                    entity != null -> LoadOutcome.Loaded(id, entity)
                    else -> LoadOutcome.Rejected(id, entityNotFoundError(id.toString()))
                }
                emit(outcome)
            }
        }
    }

    fun persistInitWithOutcomes(
        transitionContexts: Flow<InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, AUTOMATE>>
    ): Flow<PersistOutcome<EVENT>> = flow {
        transitionContexts.collect { ctx ->
            persistInit(flowOf(ctx)).collect { event ->
                emit(PersistOutcome.Success(msgId = ctx.msgId, event = event))
            }
        }
    }

    fun persistWithOutcomes(
        transitionContexts: Flow<TransitionAppliedContext<STATE, ID, ENTITY, EVENT, AUTOMATE>>
    ): Flow<PersistOutcome<EVENT>> = flow {
        transitionContexts.collect { ctx ->
            persist(flowOf(ctx)).collect { event ->
                emit(PersistOutcome.Success(msgId = ctx.msgId, event = event))
            }
        }
    }
}
