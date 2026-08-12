package s2.automate.core.engine

import f2.dsl.cqrs.envelope.Envelope
import f2.dsl.cqrs.envelope.asEnvelopeWithType
import f2.dsl.fnc.operators.mapToEnvelopeWithRandomId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import s2.automate.core.appevent.AutomateInitTransitionStarted
import s2.automate.core.appevent.AutomateSessionStopped
import s2.automate.core.appevent.AutomateTransitionEnded
import s2.automate.core.appevent.AutomateTransitionError
import s2.automate.core.appevent.AutomateTransitionStarted
import s2.automate.core.appevent.publisher.AppEventPublisher
import s2.automate.core.appevent.publisher.AutomateEventPublisher
import s2.automate.core.config.S2BatchProperties
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.InitTransitionContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.context.TransitionContext
import s2.automate.core.error.AutomateException
import s2.automate.core.guard.GuardVerifier
import s2.automate.core.persist.AutomatePersister
import s2.dsl.automate.Cmd
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2Role
import s2.dsl.automate.S2State
import s2.dsl.automate.builder.s2
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

class S2AutomateEngineImplTest {

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

    sealed interface TestEvent {
        val entityId: String
    }

    data class CreatedEvt(override val entityId: String) : TestEvent
    data class DoneEvt(override val entityId: String) : TestEvent

    private val automate: S2Automate = s2 {
        name = "EngineTest"
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

    private class StubPersister(
        private val entities: Map<String, TestEntity>,
        private val eventMapper: (TestEvent) -> TestEvent = { it },
    ) : AutomatePersister<TestState, String, TestEntity, TestEvent, S2Automate> {

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            id: String,
        ): TestEntity? = entities[id]

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            ids: Flow<String>,
        ): Flow<TestEntity?> = ids.map { entities[it] }

        override suspend fun persistInit(
            transitionContexts: Flow<InitTransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<TestEvent> = transitionContexts.map { it.event }

        override suspend fun persist(
            transitionContexts: Flow<TransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<TestEvent> = transitionContexts.map { eventMapper(it.event) }
    }

    private class RecordingPublisher : AppEventPublisher {
        val published = mutableListOf<Any>()
        override fun <EVENT> publish(event: EVENT & Any) {
            published.add(event)
        }
    }

    private class PassthroughGuardVerifier : GuardVerifier<TestState, String, TestEntity, TestEvent, S2Automate> {
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

    private fun engine(
        guard: PassthroughGuardVerifier = PassthroughGuardVerifier(),
        publisher: RecordingPublisher = RecordingPublisher(),
        entities: Map<String, TestEntity> = mapOf("1" to TestEntity("1", TestState.Created)),
        eventMapper: (TestEvent) -> TestEvent = { it },
    ): S2AutomateEngineImpl<TestState, String, TestEntity, TestEvent> {
        val automateContext = AutomateContext(automate, S2BatchProperties(size = 10))
        val automatePublisher = AutomateEventPublisher<TestState, String, TestEntity, S2Automate>(publisher)
        return S2AutomateEngineImpl(automateContext, guard, StubPersister(entities, eventMapper), automatePublisher)
    }

    @Test
    suspend fun `create decides guards and persists every command`() {
        val guard = PassthroughGuardVerifier()
        val publisher = RecordingPublisher()
        val engine = engine(guard, publisher)

        val commands = flowOf(CreateCmd("1"), CreateCmd("2")).mapToEnvelopeWithRandomId(type = "Cmd")
        val events = engine.create(commands) { cmd ->
            TestEntity(cmd.data.id, TestState.Created) to CreatedEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList()

        assertEquals(listOf("1", "2"), events.map { it.data.entityId })
        assertEquals(2, guard.evaluateInitCount)
        assertEquals(2, guard.verifyInitCount)
        assertEquals(2, publisher.published.filterIsInstance<AutomateInitTransitionStarted>().size)
    }

    @Test
    suspend fun `create maps unexpected decide failures to ERROR_UNKNOWN`() {
        val publisher = RecordingPublisher()
        val engine = engine(publisher = publisher)

        val commands = flowOf(CreateCmd("1")).mapToEnvelopeWithRandomId(type = "Cmd")
        val exception = assertThrows<AutomateException> {
            engine.create<CreateCmd, TestEntity, CreatedEvt>(commands) { _ ->
                throw IllegalStateException("decide failed")
            }.toList()
        }
        assertEquals("ERROR_UNKNOWN", exception.errors.single().type)
        assertEquals(1, publisher.published.filterIsInstance<AutomateTransitionError>().size)
    }

    @Test
    suspend fun `doTransition executes guards persists and publishes lifecycle events`() {
        val guard = PassthroughGuardVerifier()
        val publisher = RecordingPublisher()
        val engine = engine(guard, publisher)

        val commands = flowOf(DoCmd("1")).mapToEnvelopeWithRandomId(type = "Cmd")
        val events = engine.doTransition(commands) { cmd, entity ->
            TestEntity(entity.id, TestState.Active) to DoneEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList()

        assertEquals(listOf("1"), events.map { it.data.entityId })
        assertEquals(1, guard.evaluateTransitionCount)
        assertEquals(1, guard.verifyTransitionCount)
        assertEquals(1, publisher.published.filterIsInstance<AutomateTransitionStarted>().size)
        assertEquals(1, publisher.published.filterIsInstance<AutomateTransitionEnded<*, *>>().size)
        // Active is a final state: the automate session stops.
        assertEquals(1, publisher.published.filterIsInstance<AutomateSessionStopped<*>>().size)
    }

    @Test
    suspend fun `doTransition fails with ERROR_ENTITY_NOT_FOUND when the entity is missing`() {
        val engine = engine(entities = emptyMap())
        val commands = flowOf(DoCmd("missing")).mapToEnvelopeWithRandomId(type = "Cmd")
        val exception = assertThrows<AutomateException> {
            engine.doTransition(commands) { cmd, entity ->
                TestEntity(entity.id, TestState.Active) to DoneEvt(cmd.data.id).asEnvelopeWithType("Evt")
            }.toList()
        }
        assertTrue(exception.errors.single().type == "ERROR_ENTITY_NOT_FOUND")
    }

    @Test
    suspend fun `doTransition correlates equal events to their own context`() {
        val publisher = RecordingPublisher()
        val engine = engine(
            publisher = publisher,
            entities = mapOf(
                "1" to TestEntity("1", TestState.Created),
                "2" to TestEntity("2", TestState.Created),
            ),
        )

        val commands = flowOf(DoCmd("1"), DoCmd("2")).mapToEnvelopeWithRandomId(type = "Cmd")
        // Both transitions produce the very same event value: correlating by event equality
        // would route both of them to the context of command "1".
        val events = engine.doTransition(commands) { _, entity ->
            TestEntity(entity.id, TestState.Active) to DoneEvt("shared").asEnvelopeWithType("Evt")
        }.toList()

        assertEquals(listOf("shared", "shared"), events.map { it.data.entityId })
        val ended = publisher.published.filterIsInstance<AutomateTransitionEnded<*, *>>()
        assertEquals(listOf("1", "2"), ended.map { (it.entity as TestEntity).id })
        assertEquals(listOf("1", "2"), ended.map { (it.msg as DoCmd).id })
    }

    @Test
    suspend fun `doTransition still publishes lifecycle events when the persister replaces the events`() {
        val publisher = RecordingPublisher()
        val engine = engine(
            publisher = publisher,
            entities = mapOf("1" to TestEntity("1", TestState.Created)),
            eventMapper = { DoneEvt("persisted") },
        )

        val commands = flowOf(DoCmd("1")).mapToEnvelopeWithRandomId(type = "Cmd")
        val events = engine.doTransition(commands) { cmd, entity ->
            TestEntity(entity.id, TestState.Active) to DoneEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList()

        assertEquals(listOf("persisted"), events.map { it.data.entityId })
        val ended = publisher.published.filterIsInstance<AutomateTransitionEnded<*, *>>().single()
        assertEquals("1", (ended.entity as TestEntity).id)
    }
}
