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
import s2.dsl.automate.ErrorCategory
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.dsl.automate.s2error
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies that [S2AutomateStoringEvolverImpl.evolveWithOutcomes] publishes
 * [AutomatePersistFailure] for every [PersistOutcome.Failure] produced by
 * the persister (symmetric to the Success-path publish).
 */
class S2AutomateStoringEvolverImplFailurePublishTest {

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

    data class CreatedEvt(val entityId: String) : Evt
    data class DoneEvt(val entityId: String) : Evt

    // ---- stubs ----

    private class RecordingPublisher : AppEventPublisher {
        val published = mutableListOf<Any>()
        val errorEvents get() = published.filterIsInstance<AutomatePersistFailure>()
        val successEvents get() = published.filter { it !is AutomatePersistFailure }

        override fun <EVENT> publish(event: EVENT & Any) {
            published.add(event)
        }
    }

    /** Legacy engine stub (create / doTransition only — no outcome methods). */
    private class LegacyEngine : S2AutomateEngine<TestState, TestEntity, String, Evt> {

        override suspend fun <COMMAND : S2InitCommand, ENTITY_OUT : TestEntity, EVENT_OUT : Evt> create(
            commands: EnvelopedFlow<COMMAND>,
            decide: suspend (cmd: Envelope<COMMAND>) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<EVENT_OUT> = commands.map { cmd -> decide(cmd).second }

        override suspend fun <COMMAND : S2Command<String>, ENTITY_OUT : TestEntity, EVENT_OUT : Evt> doTransition(
            commands: EnvelopedFlow<COMMAND>,
            exec: suspend (Envelope<out COMMAND>, TestEntity) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<EVENT_OUT> = commands.map { cmd ->
            exec(cmd, TestEntity(cmd.data.id, TestState.Created)).second
        }
    }

    /**
     * Outcome engine stub that returns a scripted sequence of [PersistOutcome] values.
     * Cycles through [outcomes] for both init and transition paths.
     */
    private inner class ScriptedOutcomeEngine(
        private val outcomes: List<PersistOutcome<Evt>>,
    ) : S2AutomateOutcomeEngine<TestState, TestEntity, String, Evt> {

        private var idx = 0

        private fun nextOutcome(): PersistOutcome<Evt> = outcomes[idx++ % outcomes.size]

        @Suppress("UNCHECKED_CAST")
        override suspend fun <COMMAND : S2InitCommand, ENTITY_OUT : TestEntity, EVENT_OUT : Evt>
        createWithOutcomes(
            commands: EnvelopedFlow<COMMAND>,
            decide: suspend (cmd: Envelope<COMMAND>) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<PersistOutcome<EVENT_OUT>> = commands.map { cmd ->
            decide(cmd) // run the decide to satisfy callers
            nextOutcome().asEnvelopeWithType("Evt") as Envelope<PersistOutcome<EVENT_OUT>>
        }

        @Suppress("UNCHECKED_CAST")
        override suspend fun <COMMAND : S2Command<String>, ENTITY_OUT : TestEntity, EVENT_OUT : Evt>
        doTransitionWithOutcomes(
            commands: EnvelopedFlow<COMMAND>,
            exec: suspend (Envelope<out COMMAND>, TestEntity) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<PersistOutcome<EVENT_OUT>> = commands.map { cmd ->
            exec(cmd, TestEntity(cmd.data.id, TestState.Created))
            nextOutcome().asEnvelopeWithType("Evt") as Envelope<PersistOutcome<EVENT_OUT>>
        }
    }

    private fun makeEvolver(
        outcomes: List<PersistOutcome<Evt>>,
        publisher: RecordingPublisher,
    ) = S2AutomateStoringEvolverImpl(
        automateExecutor = LegacyEngine(),
        outcomeExecutor = ScriptedOutcomeEngine(outcomes),
        publisher = publisher,
        listener = AutomateEventPublisher<TestState, String, TestEntity, S2Automate>(publisher),
    )

    // ---- helpers ----

    private fun rejected(msgId: String = "c1") = PersistOutcome.Rejected<Evt>(
        msgId = msgId,
        error = s2error("ENDORSE_POLICY", "policy not met"),
    )

    private fun success() = PersistOutcome.Success<Evt>(
        msgId = "ok",
        event = CreatedEvt("e1"),
    )

    // ---- tests: init path ----

    @Test
    fun `publisher receives AutomatePersistFailure for Rejected outcome on init path`() = runTest {
        val pub = RecordingPublisher()
        val evolver = makeEvolver(listOf(rejected()), pub)

        evolver.evolveWithOutcomes(
            commands = flowOf(CreateCmd("c1")),
            idOf = { it.id },
            build = { cmd: CreateCmd ->
                TestEntity(cmd.id, TestState.Created) to CreatedEvt(cmd.id)
            }
        ).toList()

        assertEquals(1, pub.errorEvents.size)
        val err = pub.errorEvents.single()
        assertEquals("c1", err.msgId)
        assertEquals(ErrorCategory.Rejected, err.category)
        assertEquals("ENDORSE_POLICY", err.error.type)
        assertEquals("policy not met", err.error.description)
    }

    @Test
    fun `Success outcomes do NOT fire AutomatePersistFailure on init path`() = runTest {
        val pub = RecordingPublisher()
        val evolver = makeEvolver(listOf(success()), pub)

        evolver.evolveWithOutcomes(
            commands = flowOf(CreateCmd("c1")),
            idOf = { it.id },
            build = { cmd: CreateCmd ->
                TestEntity(cmd.id, TestState.Created) to CreatedEvt(cmd.id)
            }
        ).toList()

        assertEquals(0, pub.errorEvents.size, "success must not produce AutomatePersistFailure")
    }

    @Test
    fun `all 4 Failure categories produce AutomatePersistFailure with correct category string on init path`() = runTest {
        val outcomes: List<PersistOutcome<Evt>> = listOf(
            PersistOutcome.Rejected("r", s2error("RC", "msg")),
            PersistOutcome.Transient("t", s2error("TC", "msg")),
            PersistOutcome.Indeterminate("i", s2error("IC", "msg")),
            PersistOutcome.Conflict("c", s2error("CC", "msg")),
        )
        val pub = RecordingPublisher()
        val evolver = makeEvolver(outcomes, pub)

        evolver.evolveWithOutcomes(
            commands = (1..4).map { CreateCmd("id$it") }.asFlow(),
            idOf = { it.id },
            build = { cmd: CreateCmd ->
                TestEntity(cmd.id, TestState.Created) to CreatedEvt(cmd.id)
            }
        ).toList()

        assertEquals(4, pub.errorEvents.size)
        val categories = pub.errorEvents.map { it.category }
        assertEquals(
            listOf(ErrorCategory.Rejected, ErrorCategory.Transient, ErrorCategory.Indeterminate, ErrorCategory.Conflict),
            categories
        )
    }

    // ---- tests: transition path ----

    @Test
    fun `publisher receives AutomatePersistFailure for Rejected outcome on transition path`() = runTest {
        val pub = RecordingPublisher()
        val evolver = makeEvolver(listOf(rejected()), pub)

        evolver.evolveWithOutcomes(
            commands = flowOf(DoCmd("c1")),
            idOf = { it.id },
            exec = { cmd: DoCmd, entity: TestEntity ->
                entity to DoneEvt(cmd.id)
            }
        ).toList()

        assertEquals(1, pub.errorEvents.size)
        val err = pub.errorEvents.single()
        assertEquals("c1", err.msgId)
        assertEquals(ErrorCategory.Rejected, err.category)
    }

    @Test
    fun `Success outcomes do NOT fire AutomatePersistFailure on transition path`() = runTest {
        val pub = RecordingPublisher()
        val evolver = makeEvolver(listOf(success()), pub)

        evolver.evolveWithOutcomes(
            commands = flowOf(DoCmd("c1")),
            idOf = { it.id },
            exec = { cmd: DoCmd, entity: TestEntity ->
                entity to DoneEvt(cmd.id)
            }
        ).toList()

        assertEquals(0, pub.errorEvents.size, "success must not produce AutomatePersistFailure")
    }

    @Test
    fun `all 4 Failure categories produce AutomatePersistFailure with correct category string on transition path`() =
        runTest {
            val outcomes: List<PersistOutcome<Evt>> = listOf(
                PersistOutcome.Rejected("r", s2error("RC", "msg")),
                PersistOutcome.Transient("t", s2error("TC", "msg")),
                PersistOutcome.Indeterminate("i", s2error("IC", "msg")),
                PersistOutcome.Conflict("c", s2error("CC", "msg")),
            )
            val pub = RecordingPublisher()
            val evolver = makeEvolver(outcomes, pub)

            evolver.evolveWithOutcomes(
                commands = (1..4).map { DoCmd("id$it") }.asFlow(),
                idOf = { it.id },
            exec = { cmd: DoCmd, entity: TestEntity ->
                    entity to DoneEvt(cmd.id)
                }
            ).toList()

            assertEquals(4, pub.errorEvents.size)
            val categories = pub.errorEvents.map { it.category }
            assertEquals(
                listOf(
                    ErrorCategory.Rejected, ErrorCategory.Transient,
                    ErrorCategory.Indeterminate, ErrorCategory.Conflict,
                ),
                categories
            )
        }
}
