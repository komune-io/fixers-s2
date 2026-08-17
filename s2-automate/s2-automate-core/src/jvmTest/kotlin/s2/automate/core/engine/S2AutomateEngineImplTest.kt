package s2.automate.core.engine

import f2.dsl.cqrs.envelope.Envelope
import f2.dsl.cqrs.envelope.asEnvelopeWithType
import f2.dsl.fnc.operators.mapToEnvelopeWithRandomId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transform
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
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.InitTransitionContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.context.TransitionContext
import s2.automate.core.error.AutomateException
import s2.automate.core.fixtures.CreateCmd
import s2.automate.core.fixtures.CreatedEvt
import s2.automate.core.fixtures.DoCmd
import s2.automate.core.fixtures.DoneEvt
import s2.automate.core.fixtures.TestEntity
import s2.automate.core.fixtures.TestEvent
import s2.automate.core.fixtures.TestState
import s2.automate.core.fixtures.makeEngine
import s2.automate.core.fixtures.testAutomate
import s2.automate.core.guard.GuardVerifier
import s2.automate.core.persist.AutomatePersister
import s2.dsl.automate.Cmd
import s2.dsl.automate.S2Automate

class S2AutomateEngineImplTest {

    private val automate: S2Automate = testAutomate("EngineTest")

    private class StubPersister(
        private val entities: Map<String, TestEntity>,
        private val eventMapper: (TestEvent) -> TestEvent = { it },
        private val persistDecorator: (Flow<TestEvent>) -> Flow<TestEvent> = { it },
        /** Mimics a `findAllById`-style persister: ids without entity are simply not emitted. */
        private val omitMissingIds: Boolean = false,
    ) : AutomatePersister<TestState, String, TestEntity, TestEvent, S2Automate> {

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            id: String,
        ): TestEntity? = entities[id]

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            ids: Flow<String>,
        ): Flow<TestEntity?> = if (omitMissingIds) {
            ids.mapNotNull { entities[it] }
        } else {
            ids.map { entities[it] }
        }

        override suspend fun persistInit(
            transitionContexts: Flow<InitTransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<TestEvent> = transitionContexts.map { it.event }

        override suspend fun persist(
            transitionContexts: Flow<TransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<TestEvent> = persistDecorator(transitionContexts.map { eventMapper(it.event) })
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
        persistDecorator: (Flow<TestEvent>) -> Flow<TestEvent> = { it },
        omitMissingIds: Boolean = false,
    ): S2AutomateEngineImpl<TestState, String, TestEntity, TestEvent> = makeEngine(
        StubPersister(
            entities,
            eventMapper = eventMapper,
            persistDecorator = persistDecorator,
            omitMissingIds = omitMissingIds,
        ),
        guard = guard,
        publisher = publisher,
        automate = automate,
        batchSize = 10,
    )

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
        val error = exception.errors.single()
        assertTrue(error.type == "ERROR_ENTITY_NOT_FOUND")
        // The error must name the id of the command, not "null".
        assertEquals("missing", error.payload["id"])
        assertTrue(error.description.contains("missing"))
    }

    @Test
    suspend fun `doTransition fails with ERROR_ENTITY_NOT_FOUND when the persister omits the missing id`() {
        val engine = engine(entities = emptyMap(), omitMissingIds = true)
        val commands = flowOf(DoCmd("missing")).mapToEnvelopeWithRandomId(type = "Cmd")
        val exception = assertThrows<AutomateException> {
            engine.doTransition(commands) { cmd, entity ->
                TestEntity(entity.id, TestState.Active) to DoneEvt(cmd.data.id).asEnvelopeWithType("Evt")
            }.toList()
        }
        assertEquals("missing", exception.errors.single().payload["id"])
    }

    @Test
    suspend fun `doTransition does not silently drop a command whose entity is missing`() {
        val engine = engine(
            entities = mapOf("1" to TestEntity("1", TestState.Created)),
            omitMissingIds = true,
        )
        val commands = flowOf(DoCmd("1"), DoCmd("missing")).mapToEnvelopeWithRandomId(type = "Cmd")
        val exception = assertThrows<AutomateException> {
            engine.doTransition(commands) { cmd, entity ->
                TestEntity(entity.id, TestState.Active) to DoneEvt(cmd.data.id).asEnvelopeWithType("Evt")
            }.toList()
        }
        assertEquals("missing", exception.errors.single().payload["id"])
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
    suspend fun `doTransition fails when the persister emits fewer events than contexts`() {
        val publisher = RecordingPublisher()
        val engine = engine(
            publisher = publisher,
            entities = mapOf(
                "1" to TestEntity("1", TestState.Created),
                "2" to TestEntity("2", TestState.Created),
            ),
            persistDecorator = { events -> events.take(1) },
        )

        val commands = flowOf(DoCmd("1"), DoCmd("2")).mapToEnvelopeWithRandomId(type = "Cmd")
        val exception = assertThrows<AutomateException> {
            engine.doTransition(commands) { cmd, entity ->
                TestEntity(entity.id, TestState.Active) to DoneEvt(cmd.data.id).asEnvelopeWithType("Evt")
            }.toList()
        }
        val error = exception.errors.single()
        assertEquals("ERROR_PERSISTER_EVENT_COUNT", error.type)
        assertEquals("2", error.payload["expected"])
        assertEquals("1", error.payload["actual"])
    }

    @Test
    suspend fun `doTransition fails when the persister emits more events than contexts`() {
        val engine = engine(
            persistDecorator = { events ->
                events.transform { event ->
                    emit(event)
                    emit(DoneEvt("extra"))
                }
            },
        )

        val commands = flowOf(DoCmd("1")).mapToEnvelopeWithRandomId(type = "Cmd")
        val exception = assertThrows<AutomateException> {
            engine.doTransition(commands) { cmd, entity ->
                TestEntity(entity.id, TestState.Active) to DoneEvt(cmd.data.id).asEnvelopeWithType("Evt")
            }.toList()
        }
        val error = exception.errors.single()
        assertEquals("ERROR_PERSISTER_EVENT_COUNT", error.type)
        assertEquals("1", error.payload["expected"])
        assertEquals("2", error.payload["actual"])
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
