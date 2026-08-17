package s2.automate.core.storing

import f2.dsl.cqrs.envelope.Envelope
import f2.dsl.cqrs.enveloped.EnvelopedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import s2.automate.core.appevent.publisher.AutomateEventPublisher
import s2.automate.core.engine.S2AutomateEngine
import s2.automate.core.engine.S2AutomateOutcomeEngine
import s2.automate.core.error.AutomateException
import s2.automate.core.fixtures.DoCmd
import s2.automate.core.fixtures.DoneEvt
import s2.automate.core.fixtures.NoopPublisher
import s2.automate.core.fixtures.TestEntity
import s2.automate.core.fixtures.TestState
import s2.automate.core.persist.PersistOutcome
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The single-command `doTransition` used to call `Flow.first()`: an engine emitting nothing —
 * which is what happens when the entity of the command cannot be loaded — surfaced as a raw
 * `NoSuchElementException` instead of an S2 `ERROR_ENTITY_NOT_FOUND`.
 */
class S2AutomateStoringEvolverImplMissingEntityTest {

    /** Engine that emits no event at all, as when no entity matched the command. */
    private class EmptyEngine : S2AutomateEngine<TestState, TestEntity, String, Evt> {
        override suspend fun <COMMAND : S2InitCommand, ENTITY_OUT : TestEntity, EVENT_OUT : Evt> create(
            commands: EnvelopedFlow<COMMAND>,
            decide: suspend (cmd: Envelope<COMMAND>) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<EVENT_OUT> = emptyFlow()

        override suspend fun <COMMAND : S2Command<String>, ENTITY_OUT : TestEntity, EVENT_OUT : Evt> doTransition(
            commands: EnvelopedFlow<COMMAND>,
            exec: suspend (Envelope<out COMMAND>, TestEntity) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<EVENT_OUT> = emptyFlow()
    }

    private class UnusedOutcomeEngine : S2AutomateOutcomeEngine<TestState, TestEntity, String, Evt> {
        override suspend fun <COMMAND : S2InitCommand, ENTITY_OUT : TestEntity, EVENT_OUT : Evt> createWithOutcomes(
            commands: EnvelopedFlow<COMMAND>,
            decide: suspend (cmd: Envelope<COMMAND>) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<PersistOutcome<EVENT_OUT>> = error("not used in this test")

        override suspend fun <COMMAND : S2Command<String>, ENTITY_OUT : TestEntity, EVENT_OUT : Evt>
        doTransitionWithOutcomes(
            commands: EnvelopedFlow<COMMAND>,
            exec: suspend (Envelope<out COMMAND>, TestEntity) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<PersistOutcome<EVENT_OUT>> = error("not used in this test")
    }

    private fun evolver() = S2AutomateStoringEvolverImpl(
        automateExecutor = EmptyEngine(),
        outcomeExecutor = UnusedOutcomeEngine(),
        publisher = NoopPublisher(),
        listener = AutomateEventPublisher<TestState, String, TestEntity, S2Automate>(NoopPublisher()),
    )

    @Test
    fun `doTransition fails with ERROR_ENTITY_NOT_FOUND naming the command id`() = runTest {
        val exception = assertFailsWith<AutomateException> {
            evolver().doTransition(DoCmd("missing")) {
                TestEntity(id, TestState.Active) to DoneEvt(id)
            }
        }
        val error = exception.errors.single()
        assertEquals("ERROR_ENTITY_NOT_FOUND", error.type)
        assertEquals("missing", error.payload["id"])
    }
}
