package s2.automate.core.engine

import f2.dsl.cqrs.envelope.Envelope
import f2.dsl.cqrs.envelope.asEnvelopeWithType
import f2.dsl.cqrs.enveloped.EnvelopedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
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
import s2.automate.core.persist.PersistOutcome
import s2.dsl.automate.Cmd
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.builder.s2
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.dsl.automate.s2error
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the WithOutcomes paths in S2AutomateOutcomeEngineImpl:
 * - createWithOutcomes routes through persistInitWithOutcomes
 * - doTransitionWithOutcomes routes through persistWithOutcomes
 * - guards fire for every command
 * - sendEndDoTransitionEvent only fires for Committed outcomes
 * - size preservation through chunking (5 commands, batch.size=2 → 5 outcomes back)
 */
class S2AutomateOutcomeEngineImplWithOutcomesTest {

    // ---- domain fixtures ----

    enum class TestState(override var position: Int) : S2State {
        Created(0), Active(1)
    }

    object TestRole : s2.dsl.automate.S2Role

    data class TestEntity(
        val id: String,
        val state: TestState,
    ) : WithS2Id<String>, WithS2State<TestState> {
        override fun s2Id(): String = id
        override fun s2State(): TestState = state
    }

    data class CreateCmd(val id: String) : S2InitCommand
    data class DoCmd(override val id: String) : S2Command<String>

    sealed interface TestEvent {
        val entityId: String
    }

    data class CreatedEvt(override val entityId: String) : TestEvent
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

    enum class OutcomeKind { COMMITTED, REJECTED, TRANSIENT, INDETERMINATE, CONFLICT }

    /**
     * Scripted persister.
     *
     * [initPattern] / [transitionPattern] specify which kind of outcome to produce per
     * context item (cycling if pattern is shorter). For Committed, the actual context
     * event is used so commandId-based lookup works.
     */
    private class StubPersister(
        private val initPattern: List<OutcomeKind>,
        private val transitionPattern: List<OutcomeKind>,
    ) : AutomatePersister<TestState, String, TestEntity, TestEvent, S2Automate> {

        override suspend fun persistInit(
            transitionContexts: Flow<InitTransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<TestEvent> = error("persistInit must NOT be called on the WithOutcomes path")

        override suspend fun persist(
            transitionContexts: Flow<TransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<TestEvent> = error("persist must NOT be called on the WithOutcomes path")

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            ids: Flow<String>,
        ): Flow<TestEntity?> = ids.map { id -> TestEntity(id, TestState.Created) }

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            id: String,
        ): TestEntity? = TestEntity(id, TestState.Created)

        override suspend fun persistInitWithOutcomes(
            transitionContexts: Flow<InitTransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<PersistOutcome<TestEvent>> {
            val ctxs = transitionContexts.toList()
            return ctxs.mapIndexed { i, ctx ->
                toOutcome(initPattern[i % initPattern.size], ctx.event)
            }.asFlow()
        }

        override suspend fun persistWithOutcomes(
            transitionContexts: Flow<TransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<PersistOutcome<TestEvent>> {
            val ctxs = transitionContexts.toList()
            return ctxs.mapIndexed { i, ctx ->
                // Use the actual commandId from the context so B.3 correlation works correctly.
                toOutcome(transitionPattern[i % transitionPattern.size], ctx.event, ctx.msg.id.toString())
            }.asFlow()
        }

        private fun toOutcome(
            kind: OutcomeKind,
            event: TestEvent,
            commandId: String = "cmd",
        ): PersistOutcome<TestEvent> = when (kind) {
            OutcomeKind.COMMITTED -> PersistOutcome.Success(commandId, event, "tx", 1L)
            OutcomeKind.REJECTED -> PersistOutcome.Rejected(commandId, s2error("ERR", "rejected"))
            OutcomeKind.TRANSIENT -> PersistOutcome.Transient(commandId, s2error("TRANSIENT", "transient"))
            OutcomeKind.INDETERMINATE -> PersistOutcome.Indeterminate(commandId, s2error("INDET", "indeterminate"))
            OutcomeKind.CONFLICT -> PersistOutcome.Conflict(commandId, s2error("CONFLICT", "conflict"))
        }
    }

    private class RecordingAppEventPublisher : AppEventPublisher {
        val published = mutableListOf<Any>()
        override fun <EVENT> publish(event: EVENT & Any) {
            published.add(event)
        }
    }

    private class PassthroughGuardVerifier :
        GuardVerifier<TestState, String, TestEntity, TestEvent, S2Automate> {

        var evaluateInitCount = 0
        var evaluateTransitionCount = 0
        var verifyInitCount = 0
        var verifyTransitionCount = 0

        override suspend fun evaluateInit(context: InitTransitionContext<S2Automate>) {
            evaluateInitCount++
        }

        override suspend fun <COMMAND : Cmd> evaluateTransition(
            context: TransitionContext<TestState, String, TestEntity, S2Automate, COMMAND>
        ) {
            evaluateTransitionCount++
        }

        override suspend fun verifyInitTransition(
            context: InitTransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>
        ): InitTransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate> {
            verifyInitCount++
            return context
        }

        override suspend fun verifyTransition(
            context: TransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>
        ): TransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate> {
            verifyTransitionCount++
            return context
        }
    }

    // ---- helpers ----

    /** Five mixed outcome kinds: 3 Committed, 1 Rejected, 1 Transient */
    private val mixedPattern = listOf(
        OutcomeKind.COMMITTED,
        OutcomeKind.REJECTED,
        OutcomeKind.COMMITTED,
        OutcomeKind.TRANSIENT,
        OutcomeKind.COMMITTED,
    )

    private fun makeEngine(
        guard: PassthroughGuardVerifier,
        persister: StubPersister,
        publisher: RecordingAppEventPublisher,
        batchSize: Int = 2,
    ): S2AutomateOutcomeEngineImpl<TestState, String, TestEntity, TestEvent> {
        val automateContext = AutomateContext(testAutomate, S2BatchProperties(size = batchSize))
        val automateEventPublisher = AutomateEventPublisher<TestState, String, TestEntity, S2Automate>(publisher)
        return S2AutomateOutcomeEngineImpl(automateContext, guard, persister, automateEventPublisher)
    }

    // ---- tests ----

    @Test
    fun `createWithOutcomes routes through persistInitWithOutcomes`() = runTest {
        val guard = PassthroughGuardVerifier()
        val persister = StubPersister(initPattern = mixedPattern, transitionPattern = emptyList())
        val publisher = RecordingAppEventPublisher()
        // batchSize > input count so all commands flow through a single chunk
        val engine = makeEngine(guard, persister, publisher, batchSize = 10)

        val commands: EnvelopedFlow<CreateCmd> = (1..5).map { i ->
            CreateCmd("id$i").asEnvelopeWithType("Cmd")
        }.asFlow()

        val results = engine.createWithOutcomes(commands) { cmd ->
            val entity = TestEntity(cmd.data.id, TestState.Created)
            val event = CreatedEvt(cmd.data.id)
            entity to event.asEnvelopeWithType("Evt")
        }.toList()

        assertEquals(5, results.size, "Expected 5 outcomes from createWithOutcomes")
        val data = results.map { it.data }
        assertEquals(3, data.filterIsInstance<PersistOutcome.Success<*>>().size)
        assertEquals(1, data.filterIsInstance<PersistOutcome.Rejected<*>>().size)
        assertEquals(1, data.filterIsInstance<PersistOutcome.Transient<*>>().size)
    }

    @Test
    fun `doTransitionWithOutcomes routes through persistWithOutcomes`() = runTest {
        val guard = PassthroughGuardVerifier()
        val persister = StubPersister(initPattern = emptyList(), transitionPattern = mixedPattern)
        val publisher = RecordingAppEventPublisher()
        val engine = makeEngine(guard, persister, publisher, batchSize = 5)

        val commands: EnvelopedFlow<DoCmd> = (1..5).map { i ->
            DoCmd("id$i").asEnvelopeWithType("Cmd")
        }.asFlow()

        val results = engine.doTransitionWithOutcomes(commands) { cmd, entity ->
            val updatedEntity = entity.copy(state = TestState.Active)
            val event = DoneEvt(cmd.data.id)
            updatedEntity to event.asEnvelopeWithType("Evt")
        }.toList()

        assertEquals(5, results.size, "Expected 5 outcomes from doTransitionWithOutcomes")
        val data = results.map { it.data }
        assertEquals(3, data.filterIsInstance<PersistOutcome.Success<*>>().size)
        assertEquals(1, data.filterIsInstance<PersistOutcome.Rejected<*>>().size)
        assertEquals(1, data.filterIsInstance<PersistOutcome.Transient<*>>().size)
    }

    @Test
    fun `guards fire for every command in createWithOutcomes`() = runTest {
        val guard = PassthroughGuardVerifier()
        val allCommitted = listOf(OutcomeKind.COMMITTED)
        val persister = StubPersister(initPattern = allCommitted, transitionPattern = emptyList())
        val publisher = RecordingAppEventPublisher()
        val engine = makeEngine(guard, persister, publisher)

        val commands: EnvelopedFlow<CreateCmd> = (1..3).map { i ->
            CreateCmd("id$i").asEnvelopeWithType("Cmd")
        }.asFlow()

        engine.createWithOutcomes(commands) { cmd ->
            TestEntity(cmd.data.id, TestState.Created) to CreatedEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList()

        // evaluateInit fires once per command (in prepareCreationContextForOutcomes)
        assertEquals(3, guard.evaluateInitCount, "evaluateInit should fire for each command")
        // verifyInitTransition fires once per context (in persistInitWithOutcomes private helper)
        assertEquals(3, guard.verifyInitCount, "verifyInitTransition should fire for each command")
    }

    @Test
    fun `sendEndDoTransitionEvent only fires for Committed outcomes`() = runTest {
        val guard = PassthroughGuardVerifier()
        // pattern: C, R, C, T, C → 3 Committed
        val persister = StubPersister(initPattern = emptyList(), transitionPattern = mixedPattern)
        val publisher = RecordingAppEventPublisher()
        val engine = makeEngine(guard, persister, publisher, batchSize = 5)

        val commands: EnvelopedFlow<DoCmd> = (1..5).map { i ->
            DoCmd("id$i").asEnvelopeWithType("Cmd")
        }.asFlow()

        engine.doTransitionWithOutcomes(commands) { cmd, entity ->
            entity.copy(state = TestState.Active) to DoneEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList()

        // sendEndDoTransitionEvent calls publisher.automateTransitionEnded for each Committed
        val transitionEndedEvents = publisher.published
            .filterIsInstance<s2.automate.core.appevent.AutomateTransitionEnded<*, *>>()
        assertEquals(
            3,
            transitionEndedEvents.size,
            "AutomateTransitionEnded should fire exactly once per Committed outcome; " +
                "published=${publisher.published.map { it::class.simpleName }}"
        )
    }

    @Test
    fun `size preservation through chunking (5 commands, batch size 2 = 5 outcomes back)`() = runTest {
        val guard = PassthroughGuardVerifier()
        val persister = StubPersister(initPattern = emptyList(), transitionPattern = mixedPattern)
        val publisher = RecordingAppEventPublisher()
        // batch.size=2 forces chunking: chunks of [2,2,1]
        val engine = makeEngine(guard, persister, publisher, batchSize = 2)

        val commands: EnvelopedFlow<DoCmd> = (1..5).map { i ->
            DoCmd("id$i").asEnvelopeWithType("Cmd")
        }.asFlow()

        val results = engine.doTransitionWithOutcomes(commands) { cmd, entity ->
            entity.copy(state = TestState.Active) to DoneEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList()

        assertEquals(5, results.size, "All 5 outcomes must survive chunking with batch.size=2")
    }
}
