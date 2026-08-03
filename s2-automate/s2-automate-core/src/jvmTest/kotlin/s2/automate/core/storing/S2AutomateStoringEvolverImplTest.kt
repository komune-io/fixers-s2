package s2.automate.core.storing

import f2.dsl.cqrs.envelope.Envelope
import f2.dsl.cqrs.envelope.asEnvelopeWithType
import f2.dsl.cqrs.enveloped.EnvelopedFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import s2.automate.core.appevent.publisher.AppEventPublisher
import s2.automate.core.appevent.publisher.AutomateEventPublisher
import s2.automate.core.engine.S2AutomateEngine
import s2.automate.core.engine.S2AutomateOutcomeEngine
import s2.automate.core.persist.PersistOutcome
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

/**
 * Tests for the create/doTransition/evolve/evolveEnvelope paths of [S2AutomateStoringEvolverImpl].
 *
 * Pinned behaviour:
 *  - createWithEvent / doTransition return the produced event and publish its envelope
 *  - evolve keeps flow cardinality (N in → N out)
 *  - init flow overload publishes the enveloped event, transition flow overload publishes the bare event
 *  - the Decide factories delegate to the matching evolve overloads
 *  - evolveEnvelope returns enveloped events and publishes them
 */
class S2AutomateStoringEvolverImplTest {

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
    data class CreatedEvt(val entityId: String) : Evt
    data class DoneEvt(val entityId: String) : Evt

    /** Passthrough engine: applies decide/exec and forwards the produced event envelope. */
    private inner class StubEngine : S2AutomateEngine<TestState, TestEntity, String, Evt> {

        override fun <COMMAND : S2InitCommand, ENTITY_OUT : TestEntity, EVENT_OUT : Evt> create(
            commands: EnvelopedFlow<COMMAND>,
            decide: suspend (cmd: Envelope<COMMAND>) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<EVENT_OUT> = commands.map { cmd -> decide(cmd).second }

        override fun <COMMAND : S2Command<String>, ENTITY_OUT : TestEntity, EVENT_OUT : Evt> doTransition(
            commands: EnvelopedFlow<COMMAND>,
            exec: suspend (Envelope<out COMMAND>, TestEntity) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<EVENT_OUT> = commands.map { cmd ->
            val entity = TestEntity(cmd.data.id, TestState.Created)
            exec(cmd, entity).second
        }
    }

    /** The outcome engine must not be reached by the non-outcome paths under test. */
    private inner class UnreachableOutcomeEngine : S2AutomateOutcomeEngine<TestState, TestEntity, String, Evt> {

        override fun <COMMAND : S2InitCommand, ENTITY_OUT : TestEntity, EVENT_OUT : Evt> createWithOutcomes(
            commands: EnvelopedFlow<COMMAND>,
            decide: suspend (cmd: Envelope<COMMAND>) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<PersistOutcome<EVENT_OUT>> = error("outcome engine must not be used")

        override fun <COMMAND : S2Command<String>, ENTITY_OUT : TestEntity, EVENT_OUT : Evt> doTransitionWithOutcomes(
            commands: EnvelopedFlow<COMMAND>,
            exec: suspend (Envelope<out COMMAND>, TestEntity) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<PersistOutcome<EVENT_OUT>> = error("outcome engine must not be used")
    }

    private class RecordingPublisher : AppEventPublisher {
        val published = mutableListOf<Any>()
        override fun <EVENT> publish(event: EVENT & Any) {
            published.add(event)
        }
    }

    private fun makeEvolver(publisher: RecordingPublisher) = S2AutomateStoringEvolverImpl(
        automateExecutor = StubEngine(),
        outcomeExecutor = UnreachableOutcomeEngine(),
        publisher = publisher,
        listener = AutomateEventPublisher<TestState, String, TestEntity, S2Automate>(publisher),
    )

    // ---- createWithEvent ----

    @Test
    suspend fun `createWithEvent with buildEntity returns the event and publishes its envelope`() {
        val pub = RecordingPublisher()
        val evolver = makeEvolver(pub)

        val event = evolver.createWithEvent(
            command = CreateCmd("id1"),
            buildEvent = { CreatedEvt(s2Id()) },
            buildEntity = { TestEntity("id1", TestState.Created) },
        )

        assertEquals(CreatedEvt("id1"), event)
        assertEquals(1, pub.published.size)
        val published = pub.published.single()
        assertTrue(published is Envelope<*>, "createWithEvent must publish the enveloped event")
        assertEquals(CreatedEvt("id1"), (published as Envelope<*>).data)
    }

    @Test
    suspend fun `createWithEvent with pair builder returns the event and publishes its envelope`() {
        val pub = RecordingPublisher()
        val evolver = makeEvolver(pub)

        val event = evolver.createWithEvent(
            command = CreateCmd("id2"),
            build = { TestEntity("id2", TestState.Created) to CreatedEvt("id2") },
        )

        assertEquals(CreatedEvt("id2"), event)
        assertEquals(1, pub.published.size)
        assertTrue(pub.published.single() is Envelope<*>, "createWithEvent must publish the enveloped event")
    }

    // ---- doTransition ----

    @Test
    suspend fun `doTransition returns the event built from the loaded entity and publishes its envelope`() {
        val pub = RecordingPublisher()
        val evolver = makeEvolver(pub)

        val event = evolver.doTransition(
            command = DoCmd("id3"),
            exec = { copy(state = TestState.Active) to DoneEvt(s2Id()) },
        )

        assertEquals(DoneEvt("id3"), event)
        assertEquals(1, pub.published.size)
        val published = pub.published.single()
        assertTrue(published is Envelope<*>, "doTransition must publish the enveloped event")
        assertEquals(DoneEvt("id3"), (published as Envelope<*>).data)
    }

    // ---- evolve (flows) ----

    @Test
    suspend fun `evolve init flow returns one event per command and publishes envelopes`() {
        val pub = RecordingPublisher()
        val evolver = makeEvolver(pub)

        val events = evolver.evolve(
            commands = (1..3).map { CreateCmd("id$it") }.asFlow(),
            build = { cmd: CreateCmd -> TestEntity(cmd.id, TestState.Created) to CreatedEvt(cmd.id) },
        ).toList()

        assertEquals(listOf(CreatedEvt("id1"), CreatedEvt("id2"), CreatedEvt("id3")), events)
        assertEquals(3, pub.published.size)
        assertTrue(
            pub.published.all { it is Envelope<*> },
            "init flow overload must publish enveloped events",
        )
    }

    @Test
    suspend fun `evolve transition flow returns one event per command and publishes bare events`() {
        val pub = RecordingPublisher()
        val evolver = makeEvolver(pub)

        val events = evolver.evolve(
            commands = (1..3).map { DoCmd("id$it") }.asFlow(),
            exec = { cmd: DoCmd, entity: TestEntity ->
                entity.copy(state = TestState.Active) to DoneEvt(cmd.id)
            },
        ).toList()

        assertEquals(listOf(DoneEvt("id1"), DoneEvt("id2"), DoneEvt("id3")), events)
        assertEquals(3, pub.published.size)
        assertFalse(
            pub.published.any { it is Envelope<*> },
            "transition flow overload must publish the bare events",
        )
        assertTrue(pub.published.all { it is DoneEvt })
    }

    // ---- evolve (Decide factories) ----

    @Test
    suspend fun `evolve returns a Decide that runs the transition path`() {
        val pub = RecordingPublisher()
        val evolver = makeEvolver(pub)

        val decide = evolver.evolve { cmd: DoCmd, entity: TestEntity ->
            entity.copy(state = TestState.Active) to DoneEvt(cmd.id)
        }

        val events = decide(flowOf(DoCmd("id1"), DoCmd("id2"))).toList()

        assertEquals(listOf(DoneEvt("id1"), DoneEvt("id2")), events)
        assertEquals(2, pub.published.size)
    }

    @Test
    suspend fun `evolve returns a Decide that runs the init path`() {
        val pub = RecordingPublisher()
        val evolver = makeEvolver(pub)

        val decide = evolver.evolve { cmd: CreateCmd ->
            TestEntity(cmd.id, TestState.Created) to CreatedEvt(cmd.id)
        }

        val events = decide(flowOf(CreateCmd("id1"), CreateCmd("id2"))).toList()

        assertEquals(listOf(CreatedEvt("id1"), CreatedEvt("id2")), events)
        assertEquals(2, pub.published.size)
    }

    // ---- evolveEnvelope ----

    @Test
    suspend fun `evolveEnvelope init returns enveloped events and publishes them`() {
        val pub = RecordingPublisher()
        val evolver = makeEvolver(pub)

        val envelopes = evolver.evolveEnvelope(
            commands = flowOf(
                CreateCmd("id1").asEnvelopeWithType(type = "Cmd"),
                CreateCmd("id2").asEnvelopeWithType(type = "Cmd"),
            ),
            build = { cmd: CreateCmd -> TestEntity(cmd.id, TestState.Created) to CreatedEvt(cmd.id) },
        ).toList()

        assertEquals(listOf(CreatedEvt("id1"), CreatedEvt("id2")), envelopes.map { it.data })
        assertEquals(2, pub.published.size)
        assertTrue(pub.published.all { it is Envelope<*> })
    }

    @Test
    suspend fun `evolveEnvelope transition returns enveloped events and publishes them`() {
        val pub = RecordingPublisher()
        val evolver = makeEvolver(pub)

        val envelopes = evolver.evolveEnvelope(
            commands = flowOf(
                DoCmd("id1").asEnvelopeWithType(type = "Cmd"),
                DoCmd("id2").asEnvelopeWithType(type = "Cmd"),
            ),
            exec = { cmd: DoCmd, entity: TestEntity ->
                entity.copy(state = TestState.Active) to DoneEvt(cmd.id)
            },
        ).toList()

        assertEquals(listOf(DoneEvt("id1"), DoneEvt("id2")), envelopes.map { it.data })
        assertEquals(2, pub.published.size)
        assertTrue(pub.published.all { it is Envelope<*> })
    }
}
