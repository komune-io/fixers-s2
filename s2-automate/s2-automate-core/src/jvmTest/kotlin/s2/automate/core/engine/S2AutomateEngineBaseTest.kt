package s2.automate.core.engine

import f2.dsl.cqrs.envelope.Envelope
import f2.dsl.cqrs.envelope.asEnvelopeWithType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import s2.automate.core.appevent.AutomateSessionStopped
import s2.automate.core.appevent.AutomateStateExited
import s2.automate.core.appevent.AutomateTransitionEnded
import s2.automate.core.appevent.AutomateTransitionError
import s2.automate.core.appevent.AutomateTransitionStarted
import s2.automate.core.appevent.publisher.AppEventPublisher
import s2.automate.core.appevent.publisher.AutomateEventPublisher
import s2.automate.core.config.S2BatchProperties
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.error.AutomateException
import s2.automate.core.error.entityNotFoundError
import s2.automate.core.error.asException
import s2.automate.core.guard.GuardVerifier
import s2.automate.core.context.InitTransitionContext
import s2.automate.core.context.TransitionContext
import s2.automate.core.persist.AutomatePersister
import s2.automate.core.persist.LoadOutcome
import s2.automate.core.persist.PersistOutcome
import s2.dsl.automate.Cmd
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2Role
import s2.dsl.automate.S2State
import s2.dsl.automate.builder.s2
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

/**
 * Direct tests for the protected helpers of [S2AutomateEngineBase] through a
 * minimal test subclass.
 *
 * Pinned behaviour:
 *  - loadBatch pairs every command with its loaded entity, null when missing
 *  - loadBatchWithOutcomes maps Loaded/Failure/absent outcomes to Ready/Failed slots
 *  - sendEndDoTransitionEvent emits StateExited on same-state transitions and
 *    SessionStopped on final states
 *  - handleException rethrows AutomateException as-is and wraps anything else
 *    in ERROR_UNKNOWN, always publishing AutomateTransitionError
 */
class S2AutomateEngineBaseTest {

    enum class TestState(override val position: Int) : S2State {
        Created(0), Active(1)
    }

    object TestRole : S2Role

    data class TestEntity(val id: String, val state: TestState) : WithS2Id<String>, WithS2State<TestState> {
        override fun s2Id(): String = id
        override fun s2State(): TestState = state
    }

    data class CreateCmd(val id: String) : S2InitCommand
    data class DoCmd(override val id: String) : S2Command<String>
    data class DoneEvt(val entityId: String)

    private val automate: S2Automate = s2 {
        name = "EngineBaseTest"
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

    private open class StubPersister(
        private val entities: Map<String, TestEntity>,
    ) : AutomatePersister<TestState, String, TestEntity, DoneEvt, S2Automate> {

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            id: String,
        ): TestEntity? = entities[id]

        override fun load(
            automateContexts: AutomateContext<S2Automate>,
            ids: Flow<String>,
        ): Flow<TestEntity?> = ids.map { entities[it] }

        override fun persistInit(
            transitionContexts: Flow<InitTransitionAppliedContext<TestState, String, TestEntity, DoneEvt, S2Automate>>
        ): Flow<DoneEvt> = transitionContexts.map { it.event }

        override fun persist(
            transitionContexts: Flow<TransitionAppliedContext<TestState, String, TestEntity, DoneEvt, S2Automate>>
        ): Flow<DoneEvt> = transitionContexts.map { it.event }
    }

    private class RecordingPublisher : AppEventPublisher {
        val published = mutableListOf<Any>()
        override fun <EVENT> publish(event: EVENT & Any) {
            published.add(event)
        }
    }

    private class PassthroughGuardVerifier : GuardVerifier<TestState, String, TestEntity, DoneEvt, S2Automate> {
        override suspend fun evaluateInit(context: InitTransitionContext<S2Automate>) = Unit

        override suspend fun <COMMAND : Cmd> evaluateTransition(
            context: TransitionContext<TestState, String, TestEntity, S2Automate, COMMAND>
        ) = Unit

        override suspend fun verifyInitTransition(
            context: InitTransitionAppliedContext<TestState, String, TestEntity, DoneEvt, S2Automate>
        ) = context

        override suspend fun verifyTransition(
            context: TransitionAppliedContext<TestState, String, TestEntity, DoneEvt, S2Automate>
        ) = context
    }

    private class TestEngine(
        automateContext: AutomateContext<S2Automate>,
        guard: GuardVerifier<TestState, String, TestEntity, DoneEvt, S2Automate>,
        persister: AutomatePersister<TestState, String, TestEntity, DoneEvt, S2Automate>,
        publisher: AutomateEventPublisher<TestState, String, TestEntity, S2Automate>,
    ) : S2AutomateEngineBase<TestState, String, TestEntity, DoneEvt>(
        automateContext, guard, persister, publisher
    ) {
        suspend fun <COMMAND : S2Command<String>> loadTransitionContextExposed(
            commands: Flow<Envelope<COMMAND>>
        ) = loadTransitionContext(commands)

        suspend fun <COMMAND : S2Command<String>> loadBatchExposed(
            cmds: List<Envelope<COMMAND>>
        ) = loadBatch(cmds)

        suspend fun <COMMAND : S2Command<String>> loadBatchWithOutcomesExposed(
            cmds: List<Envelope<COMMAND>>
        ) = loadBatchWithOutcomes(cmds)

        fun sendEndDoTransitionEventExposed(
            to: TestState,
            fromState: TestState,
            command: S2Command<String>,
            entity: TestEntity,
        ) = sendEndDoTransitionEvent(to, fromState, command, entity)

        fun <T> handleExceptionExposed(command: Envelope<CreateCmd>, e: Exception): T =
            handleException(command, e)
    }

    private fun engine(
        publisher: RecordingPublisher = RecordingPublisher(),
        persister: AutomatePersister<TestState, String, TestEntity, DoneEvt, S2Automate> =
            StubPersister(mapOf("1" to TestEntity("1", TestState.Created))),
    ): TestEngine {
        val automateContext = AutomateContext(automate, S2BatchProperties(size = 10))
        val automatePublisher = AutomateEventPublisher<TestState, String, TestEntity, S2Automate>(publisher)
        return TestEngine(automateContext, PassthroughGuardVerifier(), persister, automatePublisher)
    }

    // ---- loadBatch ----

    @Test
    suspend fun `loadBatch pairs each command with its entity and null when missing`() {
        val engine = engine(
            persister = StubPersister(
                mapOf(
                    "1" to TestEntity("1", TestState.Created),
                    "2" to TestEntity("2", TestState.Active),
                )
            )
        )
        val cmds = listOf(
            DoCmd("1").asEnvelopeWithType(type = "Cmd"),
            DoCmd("2").asEnvelopeWithType(type = "Cmd"),
            DoCmd("missing").asEnvelopeWithType(type = "Cmd"),
        )

        val pairs = engine.loadBatchExposed(cmds)

        assertEquals(3, pairs.size)
        assertEquals(cmds.map { it.id }, pairs.map { it.first.id }, "command order must be preserved")
        assertEquals(TestEntity("1", TestState.Created), pairs[0].second)
        assertEquals(TestEntity("2", TestState.Active), pairs[1].second)
        assertNull(pairs[2].second, "missing entity must be paired with null")
    }

    // ---- loadBatchWithOutcomes ----

    @Test
    suspend fun `loadBatchWithOutcomes yields Ready for loaded and Failed-Rejected for missing entities`() {
        val engine = engine()
        val cmds = listOf(
            DoCmd("1").asEnvelopeWithType(type = "Cmd"),
            DoCmd("missing").asEnvelopeWithType(type = "Cmd"),
        )

        val slots = engine.loadBatchWithOutcomesExposed(cmds)

        assertEquals(2, slots.size)
        val ready = slots[0] as LoadedSlot.Ready
        assertEquals(TestEntity("1", TestState.Created), ready.entity)
        val failed = slots[1] as LoadedSlot.Failed
        val failure = failed.failure as PersistOutcome.Rejected
        assertEquals("ERROR_ENTITY_NOT_FOUND", failure.error.type)
    }

    @Test
    suspend fun `loadBatchWithOutcomes defaults absent outcomes to Rejected entity not found`() {
        val silentPersister = object : StubPersister(emptyMap()) {
            override fun loadWithOutcomes(
                automateContexts: AutomateContext<S2Automate>,
                ids: Flow<String>,
            ): Flow<LoadOutcome<String, TestEntity>> = emptyFlow()
        }
        val engine = engine(persister = silentPersister)
        val cmd = DoCmd("orphan").asEnvelopeWithType(type = "Cmd")

        val slots = engine.loadBatchWithOutcomesExposed(listOf(cmd))

        val failed = slots.single() as LoadedSlot.Failed
        val failure = failed.failure as PersistOutcome.Rejected
        assertEquals(cmd.id, failure.msgId, "absent outcomes must be correlated by envelope id")
        assertEquals("ERROR_ENTITY_NOT_FOUND", failure.error.type)
        assertTrue("orphan" in failure.error.description)
    }

    // ---- loadTransitionContext ----

    @Test
    suspend fun `loadTransitionContext publishes TransitionStarted and pairs entity with context`() {
        val publisher = RecordingPublisher()
        val engine = engine(publisher = publisher)

        val results = engine.loadTransitionContextExposed(
            flowOf(DoCmd("1").asEnvelopeWithType(type = "Cmd"))
        ).toList()

        assertEquals(1, results.size)
        val (entity, context) = results.single()
        assertEquals("1", entity.s2Id())
        assertEquals(TestState.Created, context.from)
        assertEquals(1, publisher.published.filterIsInstance<AutomateTransitionStarted>().size)
    }

    @Test
    suspend fun `loadTransitionContext fails when a loaded entity matches no command`() {
        // Persister answering with an entity whose id belongs to no command in the chunk.
        val misMatchedPersister = object : StubPersister(emptyMap()) {
            override fun load(
                automateContexts: AutomateContext<S2Automate>,
                ids: Flow<String>,
            ): Flow<TestEntity?> = ids.map { TestEntity("other", TestState.Created) }
        }
        val engine = engine(persister = misMatchedPersister)

        val exception = assertThrows<AutomateException> {
            engine.loadTransitionContextExposed(
                flowOf(DoCmd("1").asEnvelopeWithType(type = "Cmd"))
            ).toList()
        }
        assertEquals("ERROR_ENTITY_NOT_FOUND", exception.errors.single().type)
    }

    // ---- sendEndDoTransitionEvent ----

    @Test
    suspend fun `sendEndDoTransitionEvent emits StateExited on same-state transition`() {
        val publisher = RecordingPublisher()
        val engine = engine(publisher = publisher)
        val entity = TestEntity("1", TestState.Created)

        engine.sendEndDoTransitionEventExposed(
            to = TestState.Created,
            fromState = TestState.Created,
            command = DoCmd("1"),
            entity = entity,
        )

        assertEquals(1, publisher.published.filterIsInstance<AutomateTransitionEnded<*, *>>().size)
        assertEquals(1, publisher.published.filterIsInstance<AutomateStateExited>().size)
        // Created is not a final state: no session stop.
        assertEquals(0, publisher.published.filterIsInstance<AutomateSessionStopped<*>>().size)
    }

    @Test
    suspend fun `sendEndDoTransitionEvent emits SessionStopped when reaching a final state`() {
        val publisher = RecordingPublisher()
        val engine = engine(publisher = publisher)
        val entity = TestEntity("1", TestState.Active)

        engine.sendEndDoTransitionEventExposed(
            to = TestState.Active,
            fromState = TestState.Created,
            command = DoCmd("1"),
            entity = entity,
        )

        assertEquals(1, publisher.published.filterIsInstance<AutomateTransitionEnded<*, *>>().size)
        assertEquals(0, publisher.published.filterIsInstance<AutomateStateExited>().size)
        assertEquals(1, publisher.published.filterIsInstance<AutomateSessionStopped<*>>().size)
    }

    // ---- handleException ----

    @Test
    suspend fun `handleException rethrows AutomateException as-is and publishes the error`() {
        val publisher = RecordingPublisher()
        val engine = engine(publisher = publisher)
        val original = entityNotFoundError("1").asException()
        val command = CreateCmd("1").asEnvelopeWithType(type = "Cmd")

        val thrown = assertThrows<AutomateException> {
            engine.handleExceptionExposed<Unit>(command, original)
        }

        assertSame(original, thrown, "AutomateException must be rethrown untouched")
        assertEquals(1, publisher.published.filterIsInstance<AutomateTransitionError>().size)
    }

    @Test
    suspend fun `handleException wraps unexpected exceptions in ERROR_UNKNOWN`() {
        val publisher = RecordingPublisher()
        val engine = engine(publisher = publisher)
        val command = CreateCmd("1").asEnvelopeWithType(type = "Cmd")

        val thrown = assertThrows<AutomateException> {
            engine.handleExceptionExposed<Unit>(command, IllegalStateException("boom"))
        }

        assertEquals("ERROR_UNKNOWN", thrown.errors.single().type)
        assertEquals(1, publisher.published.filterIsInstance<AutomateTransitionError>().size)
    }
}
