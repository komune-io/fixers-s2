package s2.automate.core.storing

import f2.dsl.cqrs.envelope.Envelope
import f2.dsl.cqrs.envelope.asEnvelopeWithType
import f2.dsl.cqrs.enveloped.EnvelopedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import s2.automate.core.appevent.publisher.AppEventPublisher
import s2.automate.core.appevent.publisher.AutomateEventPublisher
import s2.dsl.automate.S2Automate
import s2.automate.core.engine.S2AutomateEngine
import s2.automate.core.engine.S2AutomateOutcomeEngine
import s2.automate.core.persist.AutomatePersistFailure
import s2.automate.core.persist.PersistOutcome
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.dsl.automate.s2error
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for the evolveWithOutcomes overloads in S2AutomateStoringEvolverImpl.
 *
 * Pinned behaviour:
 *  - N inputs → N outputs (size preservation)
 *  - publisher fires exactly once per Committed outcome (not for Rejected/Transient/Indeterminate/Conflict)
 *  - init path publishes a mapped envelope (Envelope<EVENT>)
 *  - transition path publishes the bare event (EVENT)
 */
class S2AutomateStoringEvolverImplOutcomesTest {

    // ---- domain fixtures ----

    enum class TestState(override var position: Int) : S2State {
        Created(0), Active(1)
    }

    data class TestEntity(val id: String, val state: TestState) :
        WithS2Id<String>, WithS2State<TestState> {
        override fun s2Id() = id
        override fun s2State() = state
    }

    data class CreateCmd(val id: String) : S2InitCommand
    data class DoCmd(override val id: String) : S2Command<String>

    // TestEvent is declared as a plain Evt subtype so the engine stub can be S2AutomateEngine<..., Evt>
    data class CreatedEvt(val entityId: String) : Evt
    data class DoneEvt(val entityId: String) : Evt

    // ---- outcome kinds ----

    enum class OutcomeKind { COMMITTED, REJECTED, TRANSIENT, INDETERMINATE, CONFLICT }

    private fun toOutcome(kind: OutcomeKind, event: Evt): PersistOutcome<Evt> = when (kind) {
        OutcomeKind.COMMITTED -> PersistOutcome.Success(msgId = "cmd", event = event)
        OutcomeKind.REJECTED -> PersistOutcome.Rejected("cmd", s2error("ERR", "rejected"))
        OutcomeKind.TRANSIENT -> PersistOutcome.Transient("cmd", s2error("TRANSIENT", "transient"))
        OutcomeKind.INDETERMINATE -> PersistOutcome.Indeterminate("cmd", s2error("INDET", "indeterminate"))
        OutcomeKind.CONFLICT -> PersistOutcome.Conflict("cmd", s2error("CONFLICT", "conflict"))
    }

    // ---- stub engines ----

    /** Legacy engine stub (create / doTransition only — no outcome methods). */
    private inner class LegacyStubEngine : S2AutomateEngine<TestState, TestEntity, String, Evt> {

        override suspend fun <COMMAND : S2InitCommand, ENTITY_OUT : TestEntity, EVENT_OUT : Evt> create(
            commands: EnvelopedFlow<COMMAND>,
            decide: suspend (cmd: Envelope<COMMAND>) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<EVENT_OUT> = commands.map { cmd -> decide(cmd).second }

        override suspend fun <COMMAND : S2Command<String>, ENTITY_OUT : TestEntity, EVENT_OUT : Evt> doTransition(
            commands: EnvelopedFlow<COMMAND>,
            exec: suspend (Envelope<out COMMAND>, TestEntity) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<EVENT_OUT> = commands.map { cmd ->
            val entity = TestEntity(cmd.data.id, TestState.Created)
            exec(cmd, entity).second
        }
    }

    /**
     * Outcome engine stub that uses an internal counter to cycle through the given pattern.
     */
    private inner class IndexedOutcomeStubEngine(
        private val initPattern: List<OutcomeKind>,
        private val transitionPattern: List<OutcomeKind>,
    ) : S2AutomateOutcomeEngine<TestState, TestEntity, String, Evt> {

        private var initIdx = 0
        private var transIdx = 0

        override suspend fun <COMMAND : S2InitCommand, ENTITY_OUT : TestEntity, EVENT_OUT : Evt> createWithOutcomes(
            commands: EnvelopedFlow<COMMAND>,
            decide: suspend (cmd: Envelope<COMMAND>) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<PersistOutcome<EVENT_OUT>> = commands.map { cmd ->
            val (_, evtEnvelope) = decide(cmd)
            val kind = initPattern[initIdx++ % initPattern.size]
            @Suppress("UNCHECKED_CAST")
            toOutcome(kind, evtEnvelope.data).asEnvelopeWithType("Evt") as Envelope<PersistOutcome<EVENT_OUT>>
        }

        override suspend fun <COMMAND : S2Command<String>, ENTITY_OUT : TestEntity, EVENT_OUT : Evt> doTransitionWithOutcomes(
            commands: EnvelopedFlow<COMMAND>,
            exec: suspend (Envelope<out COMMAND>, TestEntity) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<PersistOutcome<EVENT_OUT>> = commands.map { cmd ->
            val entity = TestEntity(cmd.data.id, TestState.Created)
            val (_, evtEnvelope) = exec(cmd, entity)
            val kind = transitionPattern[transIdx++ % transitionPattern.size]
            @Suppress("UNCHECKED_CAST")
            toOutcome(kind, evtEnvelope.data).asEnvelopeWithType("Evt") as Envelope<PersistOutcome<EVENT_OUT>>
        }
    }

    private class RecordingPublisher : AppEventPublisher {
        val published = mutableListOf<Any>()
        val failureEvents get() = published.filterIsInstance<AutomatePersistFailure>()
        val successEvents get() = published.filter { it !is AutomatePersistFailure }
        override fun <EVENT> publish(event: EVENT & Any) {
            published.add(event)
        }
    }

    // pattern: C, R, C, T, C → 3 committed out of 5
    private val mixedPattern = listOf(
        OutcomeKind.COMMITTED,
        OutcomeKind.REJECTED,
        OutcomeKind.COMMITTED,
        OutcomeKind.TRANSIENT,
        OutcomeKind.COMMITTED,
    )

    private val allFailing = listOf(
        OutcomeKind.REJECTED,
        OutcomeKind.TRANSIENT,
        OutcomeKind.INDETERMINATE,
        OutcomeKind.CONFLICT,
    )

    private fun makeEvolver(
        initPattern: List<OutcomeKind>,
        transitionPattern: List<OutcomeKind>,
        publisher: RecordingPublisher,
    ) = S2AutomateStoringEvolverImpl(
        automateExecutor = LegacyStubEngine(),
        outcomeExecutor = IndexedOutcomeStubEngine(initPattern, transitionPattern),
        publisher = publisher,
        listener = AutomateEventPublisher<TestState, String, TestEntity, S2Automate>(publisher),
    )

    // ---- tests: init overload ----

    @Test
    fun `evolveWithOutcomes (init) returns one outcome per input`() = runTest {
        val pub = RecordingPublisher()
        val evolver = makeEvolver(
            initPattern = listOf(OutcomeKind.COMMITTED),
            transitionPattern = emptyList(),
            publisher = pub
        )

        val results: List<PersistOutcome<CreatedEvt>> = evolver.evolveWithOutcomes(
            commands = (1..4).map { CreateCmd("id$it") }.asFlow(),
            idOf = { it.id },
            build = { cmd: CreateCmd ->
                TestEntity(cmd.id, TestState.Created) to CreatedEvt(cmd.id)
            }
        ).toList()

        assertEquals(4, results.size, "N in → N out for init path")
    }

    @Test
    fun `evolveWithOutcomes (transition) returns one outcome per input`() = runTest {
        val pub = RecordingPublisher()
        val evolver = makeEvolver(
            initPattern = emptyList(),
            transitionPattern = listOf(OutcomeKind.COMMITTED),
            publisher = pub
        )

        val results: List<PersistOutcome<DoneEvt>> = evolver.evolveWithOutcomes(
            commands = (1..4).map { DoCmd("id$it") }.asFlow(),
            idOf = { it.id },
            exec = { cmd: DoCmd, _: TestEntity ->
                TestEntity(cmd.id, TestState.Active) to DoneEvt(cmd.id)
            }
        ).toList()

        assertEquals(4, results.size, "N in → N out for transition path")
    }

    @Test
    fun `publisher fires exactly once per Committed outcome on init path`() = runTest {
        val pub = RecordingPublisher()
        val evolver = makeEvolver(
            initPattern = mixedPattern,
            transitionPattern = emptyList(),
            publisher = pub
        )

        evolver.evolveWithOutcomes(
            commands = (1..5).map { CreateCmd("id$it") }.asFlow(),
            idOf = { it.id },
            build = { cmd: CreateCmd ->
                TestEntity(cmd.id, TestState.Created) to CreatedEvt(cmd.id)
            }
        ).toList()

        assertEquals(3, pub.successEvents.size, "publisher must fire exactly 3 success events for 3 Committed outcomes")
        assertEquals(2, pub.failureEvents.size, "publisher must fire exactly 2 failure events for 2 non-Committed outcomes")
    }

    @Test
    fun `publisher fires AutomatePersistFailure for Rejected Transient Indeterminate Conflict on init path`() = runTest {
        val pub = RecordingPublisher()
        val evolver = makeEvolver(
            initPattern = allFailing,
            transitionPattern = emptyList(),
            publisher = pub
        )

        evolver.evolveWithOutcomes(
            commands = (1..4).map { CreateCmd("id$it") }.asFlow(),
            idOf = { it.id },
            build = { cmd: CreateCmd ->
                TestEntity(cmd.id, TestState.Created) to CreatedEvt(cmd.id)
            }
        ).toList()

        assertEquals(4, pub.failureEvents.size, "publisher must fire 4 AutomatePersistFailure events for 4 failure outcomes")
        assertTrue(pub.successEvents.isEmpty(), "publisher must not fire success events for failure outcomes; got: ${pub.successEvents}")
    }

    @Test
    fun `publisher fires exactly once per Committed outcome on transition path`() = runTest {
        val pub = RecordingPublisher()
        val evolver = makeEvolver(
            initPattern = emptyList(),
            transitionPattern = mixedPattern,
            publisher = pub
        )

        evolver.evolveWithOutcomes(
            commands = (1..5).map { DoCmd("id$it") }.asFlow(),
            idOf = { it.id },
            exec = { cmd: DoCmd, _: TestEntity ->
                TestEntity(cmd.id, TestState.Active) to DoneEvt(cmd.id)
            }
        ).toList()

        assertEquals(3, pub.successEvents.size, "publisher must fire exactly 3 success events for 3 Committed outcomes")
        assertEquals(2, pub.failureEvents.size, "publisher must fire exactly 2 failure events for 2 non-Committed outcomes")
    }

    @Test
    fun `publisher fires AutomatePersistFailure for Rejected Transient Indeterminate Conflict on transition path`() = runTest {
        val pub = RecordingPublisher()
        val evolver = makeEvolver(
            initPattern = emptyList(),
            transitionPattern = allFailing,
            publisher = pub
        )

        evolver.evolveWithOutcomes(
            commands = (1..4).map { DoCmd("id$it") }.asFlow(),
            idOf = { it.id },
            exec = { cmd: DoCmd, _: TestEntity ->
                TestEntity(cmd.id, TestState.Active) to DoneEvt(cmd.id)
            }
        ).toList()

        assertEquals(4, pub.failureEvents.size, "publisher must fire 4 AutomatePersistFailure events for 4 failure outcomes")
        assertTrue(pub.successEvents.isEmpty(), "publisher must not fire success events for failure outcomes; got: ${pub.successEvents}")
    }

    @Test
    fun `init path publishes mapped envelope (type=Evt)`() = runTest {
        val pub = RecordingPublisher()
        val evolver = makeEvolver(
            initPattern = listOf(OutcomeKind.COMMITTED),
            transitionPattern = emptyList(),
            publisher = pub
        )

        evolver.evolveWithOutcomes(
            commands = flowOf(CreateCmd("id1")),
            idOf = { it.id },
            build = { cmd: CreateCmd ->
                TestEntity(cmd.id, TestState.Created) to CreatedEvt(cmd.id)
            }
        ).toList()

        assertEquals(1, pub.published.size, "exactly one publish for one Committed")
        val published = pub.published.first()
        // init path: publisher.publish(envelopedOutcome.mapEnvelopeWithType({ outcome.event }, type = "Evt"))
        assertTrue(
            published is Envelope<*>,
            "init path must publish an Envelope, got: ${published::class.simpleName}"
        )
        val envelope = published as Envelope<*>
        assertEquals("Evt", envelope.type, "envelope type must be 'Evt'")
        assertTrue(
            envelope.data is Evt,
            "envelope data must be the event, got: ${envelope.data}"
        )
    }

    @Test
    fun `transition path publishes outcome event directly (not wrapped in envelope)`() = runTest {
        val pub = RecordingPublisher()
        val evolver = makeEvolver(
            initPattern = emptyList(),
            transitionPattern = listOf(OutcomeKind.COMMITTED),
            publisher = pub
        )

        evolver.evolveWithOutcomes(
            commands = flowOf(DoCmd("id1")),
            idOf = { it.id },
            exec = { cmd: DoCmd, _: TestEntity ->
                TestEntity(cmd.id, TestState.Active) to DoneEvt(cmd.id)
            }
        ).toList()

        assertEquals(1, pub.published.size, "exactly one publish for one Committed")
        val published = pub.published.first()
        // transition path: publisher.publish(outcome.event as Any)
        assertFalse(
            published is Envelope<*>,
            "transition path must publish the bare event (not an Envelope); got: ${published::class.simpleName}"
        )
        assertTrue(
            published is Evt,
            "transition path must publish the event directly; got: ${published::class.simpleName}"
        )
    }
}
