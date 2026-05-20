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
import kotlinx.coroutines.test.runTest
import s2.automate.core.appevent.publisher.AppEventPublisher
import s2.automate.core.appevent.publisher.AutomateEventPublisher
import s2.automate.core.config.S2BatchProperties
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.InitTransitionContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.context.TransitionContext
import s2.automate.core.guard.GuardVerifier
import s2.automate.core.persist.AutomatePersister
import s2.automate.core.persist.PersistOutcome
import s2.dsl.automate.Cmd
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.builder.s2
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
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

    // ---- domain fixtures ----

    enum class TestState(override var position: Int) : S2State {
        Created(0), Active(1)
    }

    object TestRole : s2.dsl.automate.S2Role

    data class TestEntity(val id: String, val state: TestState) :
        WithS2Id<String>, WithS2State<TestState> {
        override fun s2Id() = id
        override fun s2State() = state
    }

    data class CreateCmd(val id: String) : S2InitCommand
    data class DoCmd(override val id: String) : S2Command<String>

    data class CreatedEvt(val entityId: String)
    data class DoneEvt(val entityId: String)

    private val testAutomate: S2Automate = s2 {
        name = "ReactivityTestAutomate"
        init<CreateCmd> {
            to = TestState.Created
            role = TestRole
        }
        transaction<DoCmd> {
            from = TestState.Created
            to = TestState.Active
            role = TestRole
        }
    }

    // ---- stub doubles ----

    private class PassthroughGuard :
        GuardVerifier<TestState, String, TestEntity, Any, S2Automate> {

        override suspend fun evaluateInit(context: InitTransitionContext<S2Automate>) {}

        override suspend fun <COMMAND : Cmd> evaluateTransition(
            context: TransitionContext<TestState, String, TestEntity, S2Automate, COMMAND>
        ) {}

        override suspend fun verifyInitTransition(
            context: InitTransitionAppliedContext<TestState, String, TestEntity, Any, S2Automate>
        ): InitTransitionAppliedContext<TestState, String, TestEntity, Any, S2Automate> = context

        override suspend fun verifyTransition(
            context: TransitionAppliedContext<TestState, String, TestEntity, Any, S2Automate>
        ): TransitionAppliedContext<TestState, String, TestEntity, Any, S2Automate> = context
    }

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
            PersistOutcome.Success(commandId = "", event = ctx.event, transactionId = "tx", blockNumber = 1L)
        }

        override suspend fun persistInitWithOutcomes(
            transitionContexts: Flow<InitTransitionAppliedContext<
                TestState, String, TestEntity, Any, S2Automate>>
        ): Flow<PersistOutcome<Any>> = transitionContexts.map { ctx ->
            PersistOutcome.Success(commandId = "", event = ctx.event, transactionId = "tx", blockNumber = 1L)
        }
    }

    private class NoopPublisher : AppEventPublisher {
        override fun <EVENT> publish(event: EVENT & Any) {}
    }

    private fun makeEngine(
        persister: AutomatePersister<TestState, String, TestEntity, Any, S2Automate>,
        batchSize: Int,
    ): S2AutomateOutcomeEngineImpl<TestState, String, TestEntity, Any> {
        val automateContext = AutomateContext(testAutomate, S2BatchProperties(size = batchSize))
        val publisher = AutomateEventPublisher<TestState, String, TestEntity, S2Automate>(NoopPublisher())
        return S2AutomateOutcomeEngineImpl(automateContext, PassthroughGuard(), persister, publisher)
    }

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

    @Test
    fun `B1 createWithOutcomes emits first outcome before upstream completes`() = runTest {
        val persister = CountingPersister()
        // batch.size=2, slow upstream emits 6 commands with delays
        val engine = makeEngine(persister, batchSize = 2)

        val firstOutputTimeMs = longArrayOf(-1L)
        var lastInputTimeMs = -1L

        val slowCommands: EnvelopedFlow<CreateCmd> = flow {
            for (i in 1..6) {
                emit(CreateCmd("id$i").asEnvelopeWithType("Cmd"))
                lastInputTimeMs = currentTimeMs()
                if (i < 6) delay(50L) // 50ms between each command; last has no trailing delay
            }
        }

        val start = currentTimeMs()
        engine.createWithOutcomes(slowCommands) { cmd ->
            TestEntity(cmd.data.id, TestState.Created) to
                CreatedEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.collect { envelope ->
            if (firstOutputTimeMs[0] < 0L) {
                firstOutputTimeMs[0] = currentTimeMs() - start
            }
        }

        val totalInputDurationMs = lastInputTimeMs - start
        assertTrue(
            firstOutputTimeMs[0] >= 0L,
            "No output was emitted"
        )
        assertTrue(
            firstOutputTimeMs[0] < totalInputDurationMs,
            "First output (at +${firstOutputTimeMs[0]}ms) should appear before upstream finishes " +
                "(at +${totalInputDurationMs}ms) — reactivity regression B.1"
        )
    }

    private fun currentTimeMs(): Long = System.currentTimeMillis()
}
