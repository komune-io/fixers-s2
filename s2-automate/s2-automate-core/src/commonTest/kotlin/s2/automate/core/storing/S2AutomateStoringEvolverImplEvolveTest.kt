package s2.automate.core.storing

import f2.dsl.cqrs.envelope.Envelope
import f2.dsl.cqrs.enveloped.EnvelopedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import s2.automate.core.appevent.publisher.AutomateEventPublisher
import s2.automate.core.engine.S2AutomateEngine
import s2.automate.core.engine.S2AutomateOutcomeEngine
import s2.automate.core.fixtures.DoCmd
import s2.automate.core.fixtures.DoneEvt
import s2.automate.core.fixtures.NoopPublisher
import s2.automate.core.fixtures.RecordingPublisher
import s2.automate.core.fixtures.TestEntity
import s2.automate.core.fixtures.TestState
import s2.automate.core.persist.PersistOutcome
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import kotlin.test.Test
import kotlin.test.assertEquals

/** The flow `evolve` of a transition hands the engine the entity `exec` evolved, and publishes each event. */
class S2AutomateStoringEvolverImplEvolveTest {

    /** Loads every command's entity as `Created`, and records the entity `exec` returns for it. */
    private class RecordingEngine : S2AutomateEngine<TestState, TestEntity, String, Evt> {
        val evolved = mutableListOf<TestEntity>()

        override fun <COMMAND : S2InitCommand, ENTITY_OUT : TestEntity, EVENT_OUT : Evt> create(
            commands: EnvelopedFlow<COMMAND>,
            decide: suspend (cmd: Envelope<COMMAND>) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<EVENT_OUT> = error("not used in this test")

        override fun <COMMAND : S2Command<String>, ENTITY_OUT : TestEntity, EVENT_OUT : Evt> doTransition(
            commands: EnvelopedFlow<COMMAND>,
            exec: suspend (Envelope<out COMMAND>, TestEntity) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<EVENT_OUT> = commands.map { cmd ->
            val (entity, event) = exec(cmd, TestEntity(cmd.data.id, TestState.Created))
            evolved += entity
            event
        }
    }

    private class UnusedOutcomeEngine : S2AutomateOutcomeEngine<TestState, TestEntity, String, Evt> {
        override fun <COMMAND : S2InitCommand, ENTITY_OUT : TestEntity, EVENT_OUT : Evt> createWithOutcomes(
            commands: EnvelopedFlow<COMMAND>,
            decide: suspend (cmd: Envelope<COMMAND>) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<PersistOutcome<EVENT_OUT>> = error("not used in this test")

        override fun <COMMAND : S2Command<String>, ENTITY_OUT : TestEntity, EVENT_OUT : Evt>
        doTransitionWithOutcomes(
            commands: EnvelopedFlow<COMMAND>,
            exec: suspend (Envelope<out COMMAND>, TestEntity) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<PersistOutcome<EVENT_OUT>> = error("not used in this test")
    }

    @Test
    fun `transition evolve persists the evolved entities and publishes their events`() = runTest {
        val engine = RecordingEngine()
        val publisher = RecordingPublisher()
        val evolver = S2AutomateStoringEvolverImpl(
            automateExecutor = engine,
            outcomeExecutor = UnusedOutcomeEngine(),
            publisher = publisher,
            listener = AutomateEventPublisher<TestState, String, TestEntity, S2Automate>(NoopPublisher()),
        )

        val events = evolver.evolve(flowOf(DoCmd("1"), DoCmd("2"))) { cmd, entity ->
            entity.copy(state = TestState.Active) to DoneEvt(cmd.id)
        }.toList()

        assertEquals(listOf(DoneEvt("1"), DoneEvt("2")), events)
        assertEquals(listOf(TestEntity("1", TestState.Active), TestEntity("2", TestState.Active)), engine.evolved)
        assertEquals(listOf<Any>(DoneEvt("1"), DoneEvt("2")), publisher.published)
    }
}
