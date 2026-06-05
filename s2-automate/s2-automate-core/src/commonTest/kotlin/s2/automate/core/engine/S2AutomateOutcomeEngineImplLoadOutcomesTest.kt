package s2.automate.core.engine

import f2.dsl.cqrs.envelope.asEnvelopeWithType
import f2.dsl.cqrs.enveloped.EnvelopedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
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
import s2.automate.core.persist.ErrorCategory
import s2.automate.core.persist.LoadOutcome
import s2.automate.core.persist.PersistOutcome
import s2.dsl.automate.Cmd
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2Role
import s2.dsl.automate.S2State
import s2.dsl.automate.builder.s2
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.dsl.automate.s2error
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Verifies the engine's `doTransitionWithOutcomes` path consumes
 * [AutomatePersister.loadWithOutcomes] correctly and surfaces per-id load
 * failures as per-msgId [PersistOutcome.Failure] without aborting siblings:
 *  - [LoadOutcome.Rejected] → [PersistOutcome.Rejected] (permanent)
 *  - [LoadOutcome.Transient] → [PersistOutcome.Transient] (retryable)
 *  - [LoadOutcome.Loaded] → continues through buildAppliedContext/persist
 *
 * Regression test: a mixed batch where one id fails to load must still commit
 * the rest (this is the originally-broken SSM-batch scenario at the engine level).
 */
class S2AutomateOutcomeEngineImplLoadOutcomesTest {

    // ---- domain fixtures (local — convention in this repo's tests) ----

    enum class TestState(override var position: Int) : S2State {
        Created(0), Active(1)
    }

    object TestRole : S2Role

    data class TestEntity(
        val id: String,
        val state: TestState,
    ) : WithS2Id<String>, WithS2State<TestState> {
        override fun s2Id(): String = id
        override fun s2State(): TestState = state
    }

    data class CreateCmd(val id: String) : s2.dsl.automate.S2InitCommand
    data class DoCmd(override val id: String) : S2Command<String>

    sealed interface TestEvent { val entityId: String }
    data class DoneEvt(override val entityId: String) : TestEvent

    private val testAutomate: S2Automate = s2 {
        name = "TestAutomate"
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

    private class PassthroughGuardVerifier :
        GuardVerifier<TestState, String, TestEntity, TestEvent, S2Automate> {
        override suspend fun evaluateInit(context: InitTransitionContext<S2Automate>) {}
        override suspend fun <COMMAND : Cmd> evaluateTransition(
            context: TransitionContext<TestState, String, TestEntity, S2Automate, COMMAND>
        ) {}
        override suspend fun verifyInitTransition(
            context: InitTransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>
        ) = context
        override suspend fun verifyTransition(
            context: TransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>
        ) = context
    }

    /**
     * Stub persister whose [loadWithOutcomes] is overridden to emit a scripted
     * outcome per id. The legacy [load] overloads are unused (and would fail).
     * [persistWithOutcomes] passes through every transition context as Success
     * so we can observe which ids actually reached the persist step.
     */
    private class ScriptedPersister(
        private val script: Map<String, LoadOutcome<String, TestEntity>>,
    ) : AutomatePersister<TestState, String, TestEntity, TestEvent, S2Automate> {

        override suspend fun persistInit(
            transitionContexts: Flow<InitTransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<TestEvent> = error("not used")

        override suspend fun persist(
            transitionContexts: Flow<TransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<TestEvent> = error("not used")

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            ids: Flow<String>,
        ): Flow<TestEntity?> = error("legacy load should not be called when loadWithOutcomes is overridden")

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            id: String,
        ): TestEntity? = error("legacy load should not be called")

        override suspend fun loadWithOutcomes(
            automateContexts: AutomateContext<S2Automate>,
            ids: Flow<String>,
        ): Flow<LoadOutcome<String, TestEntity>> = ids.map { id ->
            script[id] ?: error("no script entry for id=$id")
        }

        override suspend fun persistWithOutcomes(
            transitionContexts: Flow<TransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<PersistOutcome<TestEvent>> = transitionContexts.map { ctx ->
            PersistOutcome.Success(msgId = ctx.msgId, event = ctx.event)
        }
    }

    /**
     * Persister whose legacy [load] throws — exercises the default
     * [loadWithOutcomes] impl's runCatching → Transient fallback through the
     * engine. Confirms the default-impl bridge works end-to-end.
     */
    private class LegacyThrowingPersister(private val cause: Throwable) :
        AutomatePersister<TestState, String, TestEntity, TestEvent, S2Automate> {

        override suspend fun persistInit(
            transitionContexts: Flow<InitTransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<TestEvent> = error("not used")

        override suspend fun persist(
            transitionContexts: Flow<TransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<TestEvent> = error("not used")

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            ids: Flow<String>,
        ): Flow<TestEntity?> = flow { throw cause }

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            id: String,
        ): TestEntity? = throw cause
    }

    private class NoopPublisher : AppEventPublisher {
        override fun <EVENT> publish(event: EVENT & Any) {}
    }

    private fun makeEngine(
        persister: AutomatePersister<TestState, String, TestEntity, TestEvent, S2Automate>,
        batchSize: Int = 10,
    ): S2AutomateOutcomeEngineImpl<TestState, String, TestEntity, TestEvent> {
        val ctx = AutomateContext(testAutomate, S2BatchProperties(size = batchSize))
        val pub = AutomateEventPublisher<TestState, String, TestEntity, S2Automate>(NoopPublisher())
        return S2AutomateOutcomeEngineImpl(ctx, PassthroughGuardVerifier(), persister, pub)
    }

    private fun cmd(id: String) = DoCmd(id).asEnvelopeWithType("Cmd", id = id)

    // ---- tests ----

    @Test
    fun `LoadOutcome Loaded proceeds through buildAppliedContext to Persist Success`() = runTest {
        val persister = ScriptedPersister(
            script = mapOf(
                "id-1" to LoadOutcome.Loaded("id-1", TestEntity("id-1", TestState.Created)),
            ),
        )
        val engine = makeEngine(persister)

        val results = engine.doTransitionWithOutcomes(flowOfCmds("id-1")) { cmd, entity ->
            entity.copy(state = TestState.Active) to
                DoneEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList()

        assertEquals(1, results.size)
        val outcome = results.single().data
        assertIs<PersistOutcome.Success<TestEvent>>(outcome,
            "Loaded must proceed to persist, got ${outcome::class.simpleName}")
        assertEquals("id-1", outcome.msgId)
    }

    @Test
    fun `LoadOutcome Rejected surfaces as PersistOutcome Rejected without calling exec`() = runTest {
        val customError = s2error("SESSION_NOT_FOUND", "no such session on chain", mapOf("id" to "id-1"))
        val persister = ScriptedPersister(
            script = mapOf("id-1" to LoadOutcome.Rejected("id-1", customError)),
        )
        val engine = makeEngine(persister)

        var execCalled = false
        val results = engine.doTransitionWithOutcomes(flowOfCmds("id-1")) { cmd, entity ->
            execCalled = true
            entity.copy(state = TestState.Active) to
                DoneEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList()

        assertEquals(1, results.size)
        val outcome = results.single().data
        val rejected = assertIs<PersistOutcome.Rejected<TestEvent>>(outcome,
            "LoadOutcome.Rejected must map to PersistOutcome.Rejected, got ${outcome::class.simpleName}")
        assertEquals("id-1", rejected.msgId, "msgId must be the envelope/correlation id")
        assertEquals("SESSION_NOT_FOUND", rejected.error.type,
            "the original S2Error.type must propagate from LoadOutcome to PersistOutcome")
        assertEquals("no such session on chain", rejected.error.description)
        assertEquals(ErrorCategory.Rejected, rejected.category)
        assertEquals(false, execCalled,
            "the decide/exec lambda must NOT run for a load-rejected item")
    }

    @Test
    fun `LoadOutcome Transient surfaces as PersistOutcome Transient (retryable)`() = runTest {
        val customError = s2error("CHAINCODE_QUERY_FAILED", "peer unreachable")
        val persister = ScriptedPersister(
            script = mapOf("id-1" to LoadOutcome.Transient("id-1", customError)),
        )
        val engine = makeEngine(persister)

        val results = engine.doTransitionWithOutcomes(flowOfCmds("id-1")) { cmd, entity ->
            entity.copy(state = TestState.Active) to
                DoneEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList()

        assertEquals(1, results.size)
        val outcome = results.single().data
        val transient = assertIs<PersistOutcome.Transient<TestEvent>>(outcome,
            "LoadOutcome.Transient must map to PersistOutcome.Transient, got ${outcome::class.simpleName}")
        assertEquals("CHAINCODE_QUERY_FAILED", transient.error.type)
        assertEquals(ErrorCategory.Transient, transient.category,
            "category must propagate so plateform-style retry policies still see 'retryable'")
    }

    @Test
    fun `mixed batch — Loaded items commit, Rejected items fail per-id, no sibling poison`() = runTest {
        // This is the regression test for the originally-broken scenario:
        // pre-fix, one bad session aborted the whole chunk. Post-fix, only the bad
        // one fails; the others reach buildAppliedContext and persist.
        val persister = ScriptedPersister(
            script = mapOf(
                "id-1" to LoadOutcome.Loaded("id-1", TestEntity("id-1", TestState.Created)),
                "id-2" to LoadOutcome.Rejected("id-2",
                    s2error("SESSION_NOT_INITIALIZED", "no logs for id-2")),
                "id-3" to LoadOutcome.Loaded("id-3", TestEntity("id-3", TestState.Created)),
            ),
        )
        val engine = makeEngine(persister)

        val results = engine.doTransitionWithOutcomes(flowOfCmds("id-1", "id-2", "id-3")) { cmd, entity ->
            entity.copy(state = TestState.Active) to
                DoneEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList()

        assertEquals(3, results.size, "every input cmd must produce exactly one outcome")
        val byMsgId = results.associate { it.data.msgId to it.data }

        assertIs<PersistOutcome.Success<TestEvent>>(byMsgId.getValue("id-1"),
            "id-1 should commit normally — load succeeded")
        assertIs<PersistOutcome.Success<TestEvent>>(byMsgId.getValue("id-3"),
            "id-3 should commit normally — load succeeded")

        val rejected = assertIs<PersistOutcome.Rejected<TestEvent>>(byMsgId.getValue("id-2"),
            "id-2 should fail with Rejected — load classified it as Rejected")
        assertEquals("SESSION_NOT_INITIALIZED", rejected.error.type,
            "the persister's specific errorCode must reach plateform unchanged")
    }

    @Test
    fun `legacy persister load throwing produces per-id Transient via default loadWithOutcomes impl`() = runTest {
        // ScriptedPersister overrides loadWithOutcomes; this test instead uses a
        // persister that throws from the legacy load(), exercising the interface's
        // default loadWithOutcomes impl that runs runCatching → Transient fallback.
        val cause = RuntimeException("backing store is down")
        val persister = LegacyThrowingPersister(cause)
        val engine = makeEngine(persister)

        val results = engine.doTransitionWithOutcomes(flowOfCmds("id-1", "id-2", "id-3")) { cmd, entity ->
            entity.copy(state = TestState.Active) to
                DoneEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList()

        assertEquals(3, results.size,
            "every input cmd must surface as a Transient — no chunk-abort exception")
        results.forEach { envelope ->
            val outcome = envelope.data
            val transient = assertIs<PersistOutcome.Transient<TestEvent>>(outcome,
                "legacy-load throw must surface as Transient (retryable) via default impl, got ${outcome::class.simpleName}")
            assertEquals("ERROR_PERSIST_LAMBDA_THROW", transient.error.type,
                "the default impl wraps the throwable in ERROR_PERSIST_LAMBDA_THROW")
        }
        val msgIds = results.map { it.data.msgId }.toSet()
        assertTrue(msgIds.containsAll(setOf("id-1", "id-2", "id-3")),
            "every input msgId must be present in the Transient outcomes; got $msgIds")
    }

    private fun flowOfCmds(vararg ids: String): EnvelopedFlow<DoCmd> =
        ids.map { cmd(it) }.asFlow()
}
