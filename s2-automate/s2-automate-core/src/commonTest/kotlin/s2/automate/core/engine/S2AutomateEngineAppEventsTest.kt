package s2.automate.core.engine

import f2.dsl.cqrs.envelope.asEnvelopeWithType
import f2.dsl.cqrs.enveloped.EnvelopedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import s2.automate.core.appevent.AutomateInitTransitionEnded
import s2.automate.core.appevent.AutomateSessionError
import s2.automate.core.appevent.AutomateSessionStarted
import s2.automate.core.appevent.AutomateSessionStopped
import s2.automate.core.appevent.AutomateStateEntered
import s2.automate.core.appevent.AutomateStateExited
import s2.automate.core.appevent.AutomateTransitionEnded
import s2.automate.core.appevent.AutomateTransitionError
import s2.automate.core.context.AutomateContext
import s2.automate.core.error.AutomateException
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
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
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

        override fun load(
            automateContexts: AutomateContext<S2Automate>,
            ids: Flow<String>,
        ): Flow<TestEntity?> = ids.map { entities[it] }

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            id: String,
        ): TestEntity? = entities[id]

        override fun persistInit(
            transitionContexts: Flow<InitTransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<TestEvent> = transitionContexts.map { it.event }

        override fun persist(
            transitionContexts: Flow<TransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<TestEvent> = transitionContexts.map { it.event }
    }

    /** Rejects every creation, so no aggregate is ever persisted. */
    private class RejectingInitPersister :
        AutomatePersister<TestState, String, TestEntity, TestEvent, S2Automate> by EchoPersister() {

        override fun persistInitWithOutcomes(
            transitionContexts: Flow<InitTransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<PersistOutcome<TestEvent>> = transitionContexts
            .map { ctx ->
                PersistOutcome.Rejected<TestEvent>(ctx.msgId, s2error(code = "ERR", description = "rejected"))
            }
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
        assertEquals(expected = 2, actual = ended.size)
        assertEquals(expected = listOf("1", "2"), actual = ended.map { (it.entity as TestEntity).id })
        assertEquals(expected = listOf("1", "2"), actual = ended.map { (it.msg as CreateCmd).id })
        assertEquals(expected = listOf(TestState.Created, TestState.Created), actual = ended.map { it.to })

        val started = publisher.eventsOf<AutomateSessionStarted<*>>()
        assertEquals(expected = 2, actual = started.size)
        assertTrue(started.all { it.automate is S2Automate })
    }

    @Test
    fun `create publishes no state entered or exited`() = runTest {
        val publisher = RecordingPublisher()
        val engine = makeEngine(EchoPersister(), publisher = publisher)

        engine.create(createCommands("1")) { cmd ->
            TestEntity(cmd.data.id, TestState.Created) to CreatedEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList()

        assertEquals(expected = 0, actual = publisher.eventsOf<AutomateStateEntered>().size)
        assertEquals(expected = 0, actual = publisher.eventsOf<AutomateStateExited>().size)
    }

    @Test
    fun `createWithOutcomes publishes init transition ended only for persisted aggregates`() = runTest {
        val publisher = RecordingPublisher()
        val engine = makeOutcomeEngine(EchoPersister(), publisher = publisher)

        engine.createWithOutcomes(createCommands("1", "2")) { cmd ->
            TestEntity(cmd.data.id, TestState.Created) to CreatedEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList()

        assertEquals(expected = 2, actual = publisher.eventsOf<AutomateInitTransitionEnded<*, *>>().size)
        assertEquals(expected = 2, actual = publisher.eventsOf<AutomateSessionStarted<*>>().size)
    }

    @Test
    fun `createWithOutcomes publishes nothing when the persister rejects the creation`() = runTest {
        val publisher = RecordingPublisher()
        val engine = makeOutcomeEngine(RejectingInitPersister(), publisher = publisher)

        val outcomes = engine.createWithOutcomes(createCommands("1")) { cmd ->
            TestEntity(cmd.data.id, TestState.Created) to CreatedEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList()

        assertEquals(expected = 1, actual = outcomes.count { it.data is PersistOutcome.Rejected })
        assertEquals(expected = 0, actual = publisher.eventsOf<AutomateInitTransitionEnded<*, *>>().size)
        assertEquals(expected = 0, actual = publisher.eventsOf<AutomateSessionStarted<*>>().size)
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
        assertEquals(expected = 1, actual = exited.size)
        assertEquals(expected = TestState.Created, actual = exited.single().state)

        val entered = publisher.eventsOf<AutomateStateEntered>()
        assertEquals(expected = 1, actual = entered.size)
        assertEquals(expected = TestState.Active, actual = entered.single().state)

        // The transition-level event is unchanged, and Active is final so the session stops.
        assertEquals(expected = 1, actual = publisher.eventsOf<AutomateTransitionEnded<*, *>>().size)
        assertEquals(expected = 1, actual = publisher.eventsOf<AutomateSessionStopped<*>>().size)
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

        assertEquals(expected = 0, actual = publisher.eventsOf<AutomateStateExited>().size)
        assertEquals(expected = 0, actual = publisher.eventsOf<AutomateStateEntered>().size)
        // The transition still ended, and Created is not final so the session keeps running.
        assertEquals(expected = 1, actual = publisher.eventsOf<AutomateTransitionEnded<*, *>>().size)
        assertEquals(expected = 0, actual = publisher.eventsOf<AutomateSessionStopped<*>>().size)
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

        assertEquals(expected = TestState.Created, actual = publisher.eventsOf<AutomateStateExited>().single().state)
        assertEquals(expected = TestState.Active, actual = publisher.eventsOf<AutomateStateEntered>().single().state)
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

        assertEquals(expected = 0, actual = publisher.eventsOf<AutomateStateExited>().size)
        assertEquals(expected = 0, actual = publisher.eventsOf<AutomateStateEntered>().size)
        assertEquals(expected = 1, actual = publisher.eventsOf<AutomateTransitionEnded<*, *>>().size)
    }

    // ---- error funnel ----

    @Test
    fun `a failing command publishes transition error and session error exactly once`() = runTest {
        val publisher = RecordingPublisher()
        val automate = testAutomate("ErroringAutomate")
        val engine = makeEngine(EchoPersister(), publisher = publisher, automate = automate)
        val boom = IllegalStateException("decide blew up")

        // handleException re-wraps the non-AutomateException throw before rethrowing it,
        // but the published events must carry the original exception.
        assertFailsWith<AutomateException> {
            engine.create<CreateCmd, TestEntity, TestEvent>(createCommands("1")) { throw boom }.toList()
        }

        val transitionErrors = publisher.eventsOf<AutomateTransitionError>()
        assertEquals(expected = 1, actual = transitionErrors.size)
        assertSame(expected = boom, actual = transitionErrors.single().exception)

        val sessionErrors = publisher.eventsOf<AutomateSessionError>()
        assertEquals(expected = 1, actual = sessionErrors.size)
        assertSame(expected = automate, actual = sessionErrors.single().automate)
        assertSame(expected = boom, actual = sessionErrors.single().exception)
    }

    /**
     * Pins the asymmetry: the outcomes engine classifies failures into [PersistOutcome] values
     * instead of throwing, so it never routes through `handleException` and publishes neither
     * error event. Wiring [AutomateSessionError] there would fire once per failed command in a
     * batch that the caller already sees as ordinary per-item outcomes.
     */
    @Test
    fun `createWithOutcomes publishes no transition error nor session error`() = runTest {
        val publisher = RecordingPublisher()
        val engine = makeOutcomeEngine(EchoPersister(), publisher = publisher)
        val boom = IllegalStateException("decide blew up")

        val outcomes = engine
            .createWithOutcomes<CreateCmd, TestEntity, TestEvent>(createCommands("1")) { throw boom }
            .toList()

        assertEquals(expected = 1, actual = outcomes.count { it.data is PersistOutcome.Indeterminate })
        assertEquals(expected = 0, actual = publisher.eventsOf<AutomateTransitionError>().size)
        assertEquals(expected = 0, actual = publisher.eventsOf<AutomateSessionError>().size)
    }
}
