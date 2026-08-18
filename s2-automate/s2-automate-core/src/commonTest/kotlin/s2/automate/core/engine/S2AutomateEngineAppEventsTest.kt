package s2.automate.core.engine

import f2.dsl.cqrs.envelope.asEnvelopeWithType
import f2.dsl.cqrs.enveloped.EnvelopedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import s2.automate.core.appevent.AutomateInitTransitionEnded
import s2.automate.core.appevent.AutomateSessionStarted
import s2.automate.core.appevent.AutomateSessionStopped
import s2.automate.core.appevent.AutomateStateEntered
import s2.automate.core.appevent.AutomateStateExited
import s2.automate.core.appevent.AutomateTransitionEnded
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.fixtures.CreateCmd
import s2.automate.core.fixtures.CreatedEvt
import s2.automate.core.fixtures.DoCmd
import s2.automate.core.fixtures.DoneEvt
import s2.automate.core.fixtures.RecordingPublisher
import s2.automate.core.fixtures.TestEntity
import s2.automate.core.fixtures.TestEvent
import s2.automate.core.fixtures.TestState
import s2.automate.core.fixtures.makeEngine
import s2.automate.core.fixtures.makeOutcomeEngine
import s2.automate.core.fixtures.selfTransitionAutomate
import s2.automate.core.fixtures.testAutomate
import s2.automate.core.persist.AutomatePersister
import s2.automate.core.persist.PersistOutcome
import s2.dsl.automate.S2Automate
import s2.dsl.automate.s2error
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the app events the engines publish around a creation and around a transition:
 * [AutomateInitTransitionEnded], [AutomateSessionStarted], [AutomateStateExited] and
 * [AutomateStateEntered]. All four used to be declared but never fired.
 *
 * [AutomateStateExited]/[AutomateStateEntered] fire only when the state actually changes,
 * so a self-transition must publish neither.
 */
class S2AutomateEngineAppEventsTest {

    /** Persister that simply echoes the context event; missing entities load as null. */
    private class EchoPersister(
        private val entities: Map<String, TestEntity> = emptyMap(),
    ) : AutomatePersister<TestState, String, TestEntity, TestEvent, S2Automate> {

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            ids: Flow<String>,
        ): Flow<TestEntity?> = ids.map { entities[it] }

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            id: String,
        ): TestEntity? = entities[id]

        override suspend fun persistInit(
            transitionContexts: Flow<InitTransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<TestEvent> = transitionContexts.map { it.event }

        override suspend fun persist(
            transitionContexts: Flow<TransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<TestEvent> = transitionContexts.map { it.event }
    }

    /** Rejects every creation, so no aggregate is ever persisted. */
    private class RejectingInitPersister :
        AutomatePersister<TestState, String, TestEntity, TestEvent, S2Automate> by EchoPersister() {

        override suspend fun persistInitWithOutcomes(
            transitionContexts: Flow<InitTransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<PersistOutcome<TestEvent>> = transitionContexts
            .map { ctx -> PersistOutcome.Rejected<TestEvent>(ctx.msgId, s2error("ERR", "rejected")) }
    }

    private fun createCommands(vararg ids: String): EnvelopedFlow<CreateCmd> =
        ids.map { CreateCmd(it).asEnvelopeWithType("Cmd") }.asFlow()

    private fun doCommands(vararg ids: String): EnvelopedFlow<DoCmd> =
        ids.map { DoCmd(it).asEnvelopeWithType("Cmd") }.asFlow()

    // ---- create ----

    @Test
    fun `create publishes init transition ended and session started once per aggregate`() = runTest {
        val publisher = RecordingPublisher()
        val engine = makeEngine(EchoPersister(), publisher = publisher)

        engine.create(createCommands("1", "2")) { cmd ->
            TestEntity(cmd.data.id, TestState.Created) to CreatedEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList()

        val ended = publisher.eventsOf<AutomateInitTransitionEnded<*, *>>()
        assertEquals(2, ended.size)
        assertEquals(listOf("1", "2"), ended.map { (it.entity as TestEntity).id })
        assertEquals(listOf("1", "2"), ended.map { (it.msg as CreateCmd).id })
        assertEquals(listOf(TestState.Created, TestState.Created), ended.map { it.to })

        val started = publisher.eventsOf<AutomateSessionStarted<*>>()
        assertEquals(2, started.size)
        assertTrue(started.all { it.automate is S2Automate })
    }

    @Test
    fun `create publishes no state entered or exited`() = runTest {
        val publisher = RecordingPublisher()
        val engine = makeEngine(EchoPersister(), publisher = publisher)

        engine.create(createCommands("1")) { cmd ->
            TestEntity(cmd.data.id, TestState.Created) to CreatedEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList()

        assertEquals(0, publisher.eventsOf<AutomateStateEntered>().size)
        assertEquals(0, publisher.eventsOf<AutomateStateExited>().size)
    }

    @Test
    fun `createWithOutcomes publishes init transition ended only for persisted aggregates`() = runTest {
        val publisher = RecordingPublisher()
        val engine = makeOutcomeEngine(EchoPersister(), publisher = publisher)

        engine.createWithOutcomes(createCommands("1", "2")) { cmd ->
            TestEntity(cmd.data.id, TestState.Created) to CreatedEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList()

        assertEquals(2, publisher.eventsOf<AutomateInitTransitionEnded<*, *>>().size)
        assertEquals(2, publisher.eventsOf<AutomateSessionStarted<*>>().size)
    }

    @Test
    fun `createWithOutcomes publishes nothing when the persister rejects the creation`() = runTest {
        val publisher = RecordingPublisher()
        val engine = makeOutcomeEngine(RejectingInitPersister(), publisher = publisher)

        val outcomes = engine.createWithOutcomes(createCommands("1")) { cmd ->
            TestEntity(cmd.data.id, TestState.Created) to CreatedEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList()

        assertEquals(1, outcomes.filter { it.data is PersistOutcome.Rejected }.size)
        assertEquals(0, publisher.eventsOf<AutomateInitTransitionEnded<*, *>>().size)
        assertEquals(0, publisher.eventsOf<AutomateSessionStarted<*>>().size)
    }

    // ---- doTransition ----

    @Test
    fun `doTransition publishes state exited then entered when the state changes`() = runTest {
        val publisher = RecordingPublisher()
        val engine = makeEngine(
            EchoPersister(mapOf("1" to TestEntity("1", TestState.Created))),
            publisher = publisher,
            automate = testAutomate(),
        )

        engine.doTransition(doCommands("1")) { cmd, entity ->
            entity.copy(state = TestState.Active) to DoneEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList()

        val exited = publisher.eventsOf<AutomateStateExited>()
        assertEquals(1, exited.size)
        assertEquals(TestState.Created, exited.single().state)

        val entered = publisher.eventsOf<AutomateStateEntered>()
        assertEquals(1, entered.size)
        assertEquals(TestState.Active, entered.single().state)

        // The transition-level event is unchanged, and Active is final so the session stops.
        assertEquals(1, publisher.eventsOf<AutomateTransitionEnded<*, *>>().size)
        assertEquals(1, publisher.eventsOf<AutomateSessionStopped<*>>().size)
    }

    @Test
    fun `doTransition publishes no state exited or entered on a self-transition`() = runTest {
        val publisher = RecordingPublisher()
        val engine = makeEngine(
            EchoPersister(mapOf("1" to TestEntity("1", TestState.Created))),
            publisher = publisher,
            automate = selfTransitionAutomate(),
        )

        engine.doTransition(doCommands("1")) { cmd, entity ->
            entity to DoneEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList()

        assertEquals(0, publisher.eventsOf<AutomateStateExited>().size)
        assertEquals(0, publisher.eventsOf<AutomateStateEntered>().size)
        // The transition still ended, and Created is not final so the session keeps running.
        assertEquals(1, publisher.eventsOf<AutomateTransitionEnded<*, *>>().size)
        assertEquals(0, publisher.eventsOf<AutomateSessionStopped<*>>().size)
    }

    @Test
    fun `doTransitionWithOutcomes publishes state exited then entered when the state changes`() = runTest {
        val publisher = RecordingPublisher()
        val engine = makeOutcomeEngine(
            EchoPersister(mapOf("1" to TestEntity("1", TestState.Created))),
            publisher = publisher,
        )

        engine.doTransitionWithOutcomes(doCommands("1")) { cmd, entity ->
            entity.copy(state = TestState.Active) to DoneEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList()

        assertEquals(TestState.Created, publisher.eventsOf<AutomateStateExited>().single().state)
        assertEquals(TestState.Active, publisher.eventsOf<AutomateStateEntered>().single().state)
    }

    @Test
    fun `doTransitionWithOutcomes publishes no state exited or entered on a self-transition`() = runTest {
        val publisher = RecordingPublisher()
        val engine = makeOutcomeEngine(
            EchoPersister(mapOf("1" to TestEntity("1", TestState.Created))),
            publisher = publisher,
            automate = selfTransitionAutomate(),
        )

        engine.doTransitionWithOutcomes(doCommands("1")) { cmd, entity ->
            entity to DoneEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList()

        assertEquals(0, publisher.eventsOf<AutomateStateExited>().size)
        assertEquals(0, publisher.eventsOf<AutomateStateEntered>().size)
        assertEquals(1, publisher.eventsOf<AutomateTransitionEnded<*, *>>().size)
    }
}
