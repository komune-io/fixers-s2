package s2.automate.core.engine

import f2.dsl.cqrs.envelope.asEnvelopeWithType
import f2.dsl.cqrs.enveloped.EnvelopedFlow
import f2.dsl.fnc.operators.mapToEnvelope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.fixtures.CreateCmd
import s2.automate.core.fixtures.CreatedEvt
import s2.automate.core.fixtures.DoCmd
import s2.automate.core.fixtures.DoneEvt
import s2.automate.core.fixtures.TestEntity
import s2.automate.core.fixtures.TestState
import s2.automate.core.fixtures.makeOutcomeEngine
import s2.automate.core.fixtures.testAutomate
import s2.automate.core.persist.AutomatePersister
import s2.automate.core.persist.PersistOutcome
import s2.dsl.automate.S2Automate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Reactivity guards for [S2AutomateOutcomeEngineImpl]:
 *
 * B.2: doTransitionWithOutcomes issues a single batched load per chunk (not one per command).
 * B.1: createWithOutcomes emits its first outcome BEFORE all upstream commands have been produced.
 */
class S2AutomateOutcomeEngineImplReactivityTest {

    // ---- stub doubles ----

    /**
     * Counting persister that tracks how many times the batched [load(ids)] overload is called.
     */
    private class CountingPersister :
        AutomatePersister<TestState, String, TestEntity, Any, S2Automate> {

        var loadCallCount = 0

        override suspend fun persistInit(
            transitionContexts: Flow<InitTransitionAppliedContext<
                TestState, String, TestEntity, Any, S2Automate>>
        ): Flow<Any> = error("not used")

        override suspend fun persist(
            transitionContexts: Flow<TransitionAppliedContext<
                TestState, String, TestEntity, Any, S2Automate>>
        ): Flow<Any> = error("not used")

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            ids: Flow<String>,
        ): Flow<TestEntity?> {
            loadCallCount++
            return ids.map { id -> TestEntity(id, TestState.Created) }
        }

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            id: String,
        ): TestEntity? = TestEntity(id, TestState.Created)

        override suspend fun persistWithOutcomes(
            transitionContexts: Flow<TransitionAppliedContext<
                TestState, String, TestEntity, Any, S2Automate>>
        ): Flow<PersistOutcome<Any>> = transitionContexts.map { ctx ->
            PersistOutcome.Success(msgId = ctx.msgId, event = ctx.event)
        }

        override suspend fun persistInitWithOutcomes(
            transitionContexts: Flow<InitTransitionAppliedContext<
                TestState, String, TestEntity, Any, S2Automate>>
        ): Flow<PersistOutcome<Any>> = transitionContexts.map { ctx ->
            PersistOutcome.Success(msgId = ctx.msgId, event = ctx.event)
        }
    }

    private fun makeEngine(
        persister: AutomatePersister<TestState, String, TestEntity, Any, S2Automate>,
        batchSize: Int,
    ): S2AutomateOutcomeEngineImpl<TestState, String, TestEntity, Any> =
        makeOutcomeEngine(persister, automate = testAutomate("ReactivityTestAutomate"), batchSize = batchSize)

    // ---- B.2 guard: single batched load per chunk ----

    @Test
    fun `B2 doTransitionWithOutcomes issues one load per chunk, not one per command`() = runTest {
        val persister = CountingPersister()
        // batch.size=5, 10 commands → 2 chunks → expect exactly 2 load(ids) calls
        val engine = makeEngine(persister, batchSize = 5)

        val commands: EnvelopedFlow<DoCmd> = (1..10).map { i ->
            DoCmd("id$i").asEnvelopeWithType("Cmd")
        }.asFlow()

        engine.doTransitionWithOutcomes(commands) { cmd, entity ->
            entity.copy(state = TestState.Active) to
                DoneEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList()

        assertEquals(
            2,
            persister.loadCallCount,
            "Expected exactly 2 batched load(ids) calls (one per chunk of 5), " +
                "got ${persister.loadCallCount} — likely regressed to per-command load"
        )
    }

    // ---- B.1 guard: first output before upstream is exhausted ----

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `B1 createWithOutcomes emits first outcome before upstream completes`() = runTest {
        val persister = CountingPersister()
        // batch.size=2, slow upstream emits 6 commands with delays
        val engine = makeEngine(persister, batchSize = 2)

        // Capture TestScope's virtual clock for use inside the flow builder
        // (inside `flow { }` the receiver is FlowCollector, not TestScope).
        val scheduler = testScheduler
        val virtualNow: () -> Long = { scheduler.currentTime }

        var firstOutputTimeMs = -1L
        var lastInputTimeMs = -1L

        val slowCommands: EnvelopedFlow<CreateCmd> = flow {
            for (i in 1..6) {
                emit(CreateCmd("id$i").asEnvelopeWithType("Cmd"))
                lastInputTimeMs = virtualNow()
                if (i < 6) delay(50L) // 50ms between each command; last has no trailing delay
            }
        }

        val start = virtualNow()
        engine.createWithOutcomes(slowCommands) { cmd ->
            TestEntity(cmd.data.id, TestState.Created) to
                CreatedEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.collect {
            if (firstOutputTimeMs < 0L) {
                firstOutputTimeMs = virtualNow() - start
            }
        }

        val totalInputDurationMs = lastInputTimeMs - start
        assertTrue(
            firstOutputTimeMs >= 0L,
            "No output was emitted"
        )
        assertTrue(
            firstOutputTimeMs < totalInputDurationMs,
            "First output (at +${firstOutputTimeMs}ms virtual) should appear before upstream " +
                "finishes (at +${totalInputDurationMs}ms virtual) — reactivity regression B.1"
        )
    }
}
