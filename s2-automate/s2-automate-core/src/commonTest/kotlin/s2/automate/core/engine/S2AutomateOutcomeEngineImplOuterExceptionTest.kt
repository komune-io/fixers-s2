package s2.automate.core.engine

import f2.dsl.cqrs.envelope.Envelope
import f2.dsl.cqrs.envelope.asEnvelopeWithType
import f2.dsl.cqrs.enveloped.EnvelopedFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.InitTransitionContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.context.TransitionContext
import s2.automate.core.error.invalidTransitionError
import s2.automate.core.error.asException
import s2.automate.core.fixtures.CreateCmd
import s2.automate.core.fixtures.CreatedEvt
import s2.automate.core.fixtures.DoCmd
import s2.automate.core.fixtures.DoneEvt
import s2.automate.core.fixtures.PassthroughGuard
import s2.automate.core.fixtures.TestEntity
import s2.automate.core.fixtures.TestEvent
import s2.automate.core.fixtures.TestState
import s2.automate.core.fixtures.makeOutcomeEngine
import s2.automate.core.guard.GuardVerifier
import s2.automate.core.persist.AutomatePersister
import s2.automate.core.persist.PersistOutcome
import s2.dsl.automate.Cmd
import s2.dsl.automate.S2Automate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Specific exception used in tests to simulate a decide-lambda failure. */
private class DecideExplodedException(message: String) : Exception(message)

/** Specific exception used in tests to simulate an exec-lambda failure. */
private class ExecExplodedException(message: String) : Exception(message)

/**
 * Tests that outer exceptions (guard rejections, entity-not-found, decide/exec lambda throws)
 * surface as per-msgId PersistOutcome.Rejected / Indeterminate instead of aborting the
 * whole batch with an exception.
 */
class S2AutomateOutcomeEngineImplOuterExceptionTest {

    // ---- stub doubles ----

    /** Guard that always rejects evaluateInit and evaluateTransition. */
    private class RejectingGuardVerifier :
        GuardVerifier<TestState, String, TestEntity, TestEvent, S2Automate> {

        override suspend fun evaluateInit(context: InitTransitionContext<S2Automate>) {
            throw invalidTransitionError("any", "guard-reject").asException()
        }

        override suspend fun <COMMAND : Cmd> evaluateTransition(
            context: TransitionContext<TestState, String, TestEntity, S2Automate, COMMAND>
        ) {
            throw invalidTransitionError("any", "guard-reject").asException()
        }

        override suspend fun verifyInitTransition(
            context: InitTransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>
        ): InitTransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate> = context

        override suspend fun verifyTransition(
            context: TransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>
        ): TransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate> = context
    }

    /** Persister that always returns no entities (simulates entity-not-found). */
    private class EmptyPersister :
        AutomatePersister<TestState, String, TestEntity, TestEvent, S2Automate> {

        override suspend fun persistInit(
            transitionContexts: Flow<InitTransitionAppliedContext<
                TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<TestEvent> = error("not used")

        override suspend fun persist(
            transitionContexts: Flow<TransitionAppliedContext<
                TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<TestEvent> = error("not used")

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            ids: Flow<String>,
        ): Flow<TestEntity?> = ids.map { null }

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            id: String,
        ): TestEntity? = null
    }

    /** Persister that accepts transitions but simply returns a success outcome per context. */
    private class PassthroughPersister :
        AutomatePersister<TestState, String, TestEntity, TestEvent, S2Automate> {

        override suspend fun persistInit(
            transitionContexts: Flow<InitTransitionAppliedContext<
                TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<TestEvent> = error("not used")

        override suspend fun persist(
            transitionContexts: Flow<TransitionAppliedContext<
                TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<TestEvent> = error("not used")

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            ids: Flow<String>,
        ): Flow<TestEntity?> = ids.map { id -> TestEntity(id, TestState.Created) }

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            id: String,
        ): TestEntity? = TestEntity(id, TestState.Created)

        override suspend fun persistInitWithOutcomes(
            transitionContexts: Flow<InitTransitionAppliedContext<
                TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<PersistOutcome<TestEvent>> = transitionContexts.map { ctx ->
            PersistOutcome.Success(
                msgId = ctx.msgId,
                event = ctx.event,
            )
        }

        override suspend fun persistWithOutcomes(
            transitionContexts: Flow<TransitionAppliedContext<
                TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<PersistOutcome<TestEvent>> = transitionContexts.map { ctx ->
            PersistOutcome.Success(
                msgId = ctx.msgId,
                event = ctx.event,
            )
        }
    }

    // ---- helpers ----

    private fun makeEngine(
        guard: GuardVerifier<TestState, String, TestEntity, TestEvent, S2Automate>,
        persister: AutomatePersister<TestState, String, TestEntity, TestEvent, S2Automate>,
        batchSize: Int = 10,
    ): S2AutomateOutcomeEngineImpl<TestState, String, TestEntity, TestEvent> =
        makeOutcomeEngine(persister, guard = guard, batchSize = batchSize)

    // ---- tests ----

    @Test
    fun `guard rejection on init surfaces as PersistOutcome Rejected, not thrown`() = runTest {
        val engine = makeEngine(
            guard = RejectingGuardVerifier(),
            persister = PassthroughPersister(),
        )

        val cmdIds = listOf("id1", "id2")
        val commands: EnvelopedFlow<CreateCmd> = cmdIds.map { id ->
            CreateCmd(id).asEnvelopeWithType("Cmd", id = id)
        }.asFlow()

        val results = engine.createWithOutcomes(commands) { cmd ->
            TestEntity(cmd.data.id, TestState.Created) to
                CreatedEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList()

        assertEquals(2, results.size, "Expected one outcome per command, not a thrown exception")
        results.forEach { envelope ->
            val outcome = envelope.data
            assertIs<PersistOutcome.Rejected<*>>(
                outcome,
                "Expected Rejected but got ${outcome::class.simpleName} for msgId=${outcome.msgId}"
            )
            assertEquals(
                "ERROR_INVALID_TRANSITION",
                outcome.error.type,
                "Guard-rejection error.type must be ERROR_INVALID_TRANSITION"
            )
        }
        val msgIds = results.map { it.data.msgId }.toSet()
        assertTrue(
            msgIds.containsAll(setOf("id1", "id2")),
            "msgIds in outcomes should match the input commands; got $msgIds"
        )
    }

    @Test
    fun `guard rejection on transition surfaces per-msgId, not whole-batch throw`() = runTest {
        val engine = makeEngine(
            guard = RejectingGuardVerifier(),
            persister = PassthroughPersister(),
        )

        val cmdIds = listOf("id1", "id2", "id3")
        val commands: EnvelopedFlow<DoCmd> = cmdIds.map { id ->
            DoCmd(id).asEnvelopeWithType("Cmd", id = id)
        }.asFlow()

        val results = engine.doTransitionWithOutcomes(commands) { cmd, entity ->
            entity.copy(state = TestState.Active) to
                DoneEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList()

        assertEquals(3, results.size, "Expected one outcome per command, not a thrown exception")
        results.forEach { envelope ->
            val outcome = envelope.data
            assertIs<PersistOutcome.Rejected<*>>(
                outcome,
                "Expected Rejected but got ${outcome::class.simpleName} for msgId=${outcome.msgId}"
            )
            assertEquals(
                "ERROR_INVALID_TRANSITION",
                outcome.error.type,
                "Guard-rejection error.type must be ERROR_INVALID_TRANSITION"
            )
        }
        val msgIds = results.map { it.data.msgId }.toSet()
        assertTrue(
            msgIds.containsAll(setOf("id1", "id2", "id3")),
            "msgIds in outcomes should match the input commands; got $msgIds"
        )
    }

    @Test
    fun `entity-not-found surfaces as Rejected with ENTITY_NOT_FOUND code`() = runTest {
        val engine = makeEngine(
            guard = PassthroughGuard(),
            persister = EmptyPersister(),
        )

        val commands: EnvelopedFlow<DoCmd> = flowOf(
            DoCmd("missing-id").asEnvelopeWithType("Cmd", id = "missing-id")
        )

        val results = engine.doTransitionWithOutcomes(commands) { cmd, entity ->
            entity.copy(state = TestState.Active) to
                DoneEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList()

        assertEquals(1, results.size, "Expected one outcome, not a thrown exception")
        val outcome = results.single().data
        assertIs<PersistOutcome.Rejected<*>>(
            outcome,
            "entity-not-found must surface as Rejected, got ${outcome::class.simpleName}"
        )
        assertEquals(
            "ERROR_ENTITY_NOT_FOUND",
            outcome.error.type,
            "error.type must be ERROR_ENTITY_NOT_FOUND"
        )
    }

    @Test
    fun `decide lambda throw becomes Indeterminate, not bubble`() = runTest {
        val engine = makeEngine(
            guard = PassthroughGuard(),
            persister = PassthroughPersister(),
        )

        val commands: EnvelopedFlow<CreateCmd> = flowOf(
            CreateCmd("id1").asEnvelopeWithType("Cmd", id = "id1")
        )

        val results = engine.createWithOutcomes(commands) { _ ->
            throw DecideExplodedException("decide exploded")
            @Suppress("UNREACHABLE_CODE")
            TestEntity("x", TestState.Created) to
                CreatedEvt("x").asEnvelopeWithType("Evt")
        }.toList()

        assertEquals(1, results.size, "Expected one outcome, not a thrown exception")
        val outcome = results.single().data
        assertIs<PersistOutcome.Indeterminate<*>>(
            outcome,
            "lambda throw must surface as Indeterminate, got ${outcome::class.simpleName}"
        )
        assertEquals(
            "ERROR_PERSIST_LAMBDA_THROW",
            outcome.error.type,
            "error.type must be ERROR_PERSIST_LAMBDA_THROW"
        )
    }

    @Test
    fun `exec lambda throw on transition becomes Indeterminate, not bubble`() = runTest {
        val engine = makeEngine(
            guard = PassthroughGuard(),
            persister = PassthroughPersister(),
        )

        val commands: EnvelopedFlow<DoCmd> = flowOf(
            DoCmd("cmd-1").asEnvelopeWithType("Cmd", id = "cmd-1")
        )

        val results = engine.doTransitionWithOutcomes(commands) { _, _ ->
            throw ExecExplodedException("exec exploded")
            @Suppress("UNREACHABLE_CODE")
            TestEntity("cmd-1", TestState.Active) to
                DoneEvt("cmd-1").asEnvelopeWithType("Evt")
        }.toList()

        assertEquals(1, results.size, "Expected one outcome, not a thrown exception")
        val outcome = results.single().data
        assertIs<PersistOutcome.Indeterminate<*>>(
            outcome,
            "exec lambda throw must surface as Indeterminate, got ${outcome::class.simpleName}"
        )
        assertEquals(
            "ERROR_PERSIST_LAMBDA_THROW",
            outcome.error.type,
            "error.type must be ERROR_PERSIST_LAMBDA_THROW"
        )
        assertEquals(
            "cmd-1",
            outcome.msgId,
            "msgId must match the input command"
        )
        assertTrue(
            "exec exploded" in outcome.error.description,
            "error.description must contain the exception message"
        )
    }

    @Test
    fun `createWithOutcomes - CancellationException from decide lambda propagates instead of becoming Indeterminate`() = runTest {
        // The runCatching in partitionCreations previously caught CancellationException
        // and converted it to a per-id Indeterminate outcome — silently swallowing the
        // cooperative cancellation signal so the parent scope never unwound.
        //
        // Post-fix the catch is explicit `try/catch (CancellationException) throw e`,
        // so the cancellation propagates up the coroutine tree. We don't assert
        // instance identity because Kotlin's structured-concurrency machinery
        // (`flattenConcurrently`, `runTest`) may re-throw a fresh CancellationException
        // carrying the same message as it unwinds — what matters is:
        //   - the throw escapes (no PersistOutcome emitted)
        //   - it's still a CancellationException (no wrapping into RuntimeException)
        //   - the original message survives
        val engine = makeEngine(
            guard = PassthroughGuard(),
            persister = PassthroughPersister(),
        )

        val commands: EnvelopedFlow<CreateCmd> = flowOf(
            CreateCmd("id1").asEnvelopeWithType("Cmd", id = "id1")
        )

        val cancel = CancellationException("cooperative cancel inside decide")

        val thrown = assertFailsWith<CancellationException> {
            engine.createWithOutcomes(commands) { _ ->
                throw cancel
                @Suppress("UNREACHABLE_CODE")
                TestEntity("x", TestState.Created) to
                    CreatedEvt("x").asEnvelopeWithType("Evt")
            }.toList()
        }
        assertEquals(cancel.message, thrown.message,
            "original cancellation message must survive — confirms our cancel reached the unwind, not a different one")
    }

    @Test
    fun `doTransitionWithOutcomes - CancellationException from exec lambda propagates instead of becoming Indeterminate`() = runTest {
        // Same expectation for partitionTransitions: CancellationException thrown
        // by the exec lambda (or by guardExecutor.evaluateTransition) must propagate
        // up the coroutine tree rather than being mapped to Indeterminate.
        val engine = makeEngine(
            guard = PassthroughGuard(),
            persister = PassthroughPersister(),
        )

        val commands: EnvelopedFlow<DoCmd> = flowOf(
            DoCmd("cmd-1").asEnvelopeWithType("Cmd", id = "cmd-1")
        )

        val cancel = CancellationException("cooperative cancel inside exec")

        val thrown = assertFailsWith<CancellationException> {
            engine.doTransitionWithOutcomes(commands) { _, _ ->
                throw cancel
                @Suppress("UNREACHABLE_CODE")
                TestEntity("cmd-1", TestState.Active) to
                    DoneEvt("cmd-1").asEnvelopeWithType("Evt")
            }.toList()
        }
        assertEquals(cancel.message, thrown.message,
            "original cancellation message must survive — confirms our cancel reached the unwind, not a different one")
    }
}
