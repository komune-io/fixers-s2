package s2.automate.core.storing

import f2.dsl.cqrs.envelope.Envelope
import f2.dsl.cqrs.envelope.asEnvelopeWithType
import f2.dsl.cqrs.enveloped.EnvelopedFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
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
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the new `idOf` parameter on [S2AutomateStoringEvolverImpl.evolveWithOutcomes].
 *
 * Without the fix, the engine wrapped commands in `mapToEnvelope(type = "Cmd")` which
 * defaulted to a random UUID — so [PersistOutcome.msgId] returned to the caller had no
 * relationship to the input command's `msgId` field. Downstream callers (notably
 * `ssm-delivery`'s `DeliveryDeciderImpl`) then failed to look up the source command via
 * `cmdsByMsgId[outcome.msgId]`, producing blank identity on failure events.
 *
 * After the fix, callers supply an `idOf` extractor. These tests assert the extractor
 * is the value flowing through to `outcome.msgId`.
 */
class S2AutomateStoringEvolverImplIdOfTest {

    enum class TestState(override var position: Int) : S2State {
        Created(0), Active(1)
    }

    data class TestEntity(val id: String, val state: TestState) :
        WithS2Id<String>, WithS2State<TestState> {
        override fun s2Id() = id
        override fun s2State() = state
    }

    /** Note: command's `id` (for S2Command) and application `msgId` are intentionally distinct. */
    data class CreateCmd(val id: String, val msgId: String) : S2InitCommand
    data class DoCmd(override val id: String, val msgId: String) : S2Command<String>

    data class CreatedEvt(val entityId: String) : Evt
    data class DoneEvt(val entityId: String) : Evt

    /**
     * Echoes the inbound envelope id back as the outcome's msgId. This is the contract the
     * engine + persister stack actually satisfies in production — every code path uses
     * `transitionContext.command.id` / `cmd.id` (the envelope id) as `PersistOutcome.msgId`.
     */
    private class EnvelopeIdEchoOutcomeEngine : S2AutomateOutcomeEngine<TestState, TestEntity, String, Evt> {

        override suspend fun <COMMAND : S2InitCommand, ENTITY_OUT : TestEntity, EVENT_OUT : Evt> createWithOutcomes(
            commands: EnvelopedFlow<COMMAND>,
            decide: suspend (cmd: Envelope<COMMAND>) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<PersistOutcome<EVENT_OUT>> = commands.map { cmd ->
            val (_, evtEnvelope) = decide(cmd)
            val outcome: PersistOutcome<EVENT_OUT> = PersistOutcome.Success(msgId = cmd.id, event = evtEnvelope.data)
            outcome.asEnvelopeWithType("PersistOutcome")
        }

        override suspend fun <COMMAND : S2Command<String>, ENTITY_OUT : TestEntity, EVENT_OUT : Evt> doTransitionWithOutcomes(
            commands: EnvelopedFlow<COMMAND>,
            exec: suspend (Envelope<out COMMAND>, TestEntity) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<PersistOutcome<EVENT_OUT>> = commands.map { cmd ->
            val entity = TestEntity(cmd.data.id, TestState.Created)
            val (_, evtEnvelope) = exec(cmd, entity)
            val outcome: PersistOutcome<EVENT_OUT> = PersistOutcome.Success(msgId = cmd.id, event = evtEnvelope.data)
            outcome.asEnvelopeWithType("PersistOutcome")
        }
    }

    /** Minimal legacy-engine stub so the impl can be instantiated. Not exercised by these tests. */
    private class UnusedLegacyEngine : S2AutomateEngine<TestState, TestEntity, String, Evt> {
        override suspend fun <COMMAND : S2InitCommand, ENTITY_OUT : TestEntity, EVENT_OUT : Evt> create(
            commands: EnvelopedFlow<COMMAND>,
            decide: suspend (cmd: Envelope<COMMAND>) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<EVENT_OUT> = error("not used in idOf propagation tests")

        override suspend fun <COMMAND : S2Command<String>, ENTITY_OUT : TestEntity, EVENT_OUT : Evt> doTransition(
            commands: EnvelopedFlow<COMMAND>,
            exec: suspend (Envelope<out COMMAND>, TestEntity) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<EVENT_OUT> = error("not used in idOf propagation tests")
    }

    private class NoopPublisher : AppEventPublisher {
        override fun <EVENT> publish(event: EVENT & Any) = Unit
    }

    private fun makeEvolver() = S2AutomateStoringEvolverImpl(
        automateExecutor = UnusedLegacyEngine(),
        outcomeExecutor = EnvelopeIdEchoOutcomeEngine(),
        publisher = NoopPublisher(),
        listener = AutomateEventPublisher<TestState, String, TestEntity, S2Automate>(NoopPublisher()),
    )

    @Test
    fun `evolveWithOutcomes init - outcome msgId equals idOf cmd`() = runTest {
        val evolver = makeEvolver()
        val cmds = (1..3).map { CreateCmd(id = "ent-$it", msgId = "msg-$it") }

        val outcomes: List<PersistOutcome<CreatedEvt>> = evolver.evolveWithOutcomes(
            commands = cmds.asFlow(),
            idOf = { it.msgId },
        ) { cmd ->
            TestEntity(cmd.id, TestState.Created) to CreatedEvt(cmd.id)
        }.toList()

        assertEquals(listOf("msg-1", "msg-2", "msg-3"), outcomes.map { it.msgId })
    }

    @Test
    fun `evolveWithOutcomes transition - outcome msgId equals idOf cmd`() = runTest {
        val evolver = makeEvolver()
        val cmds = (1..3).map { DoCmd(id = "ent-$it", msgId = "msg-$it") }

        val outcomes: List<PersistOutcome<DoneEvt>> = evolver.evolveWithOutcomes(
            commands = cmds.asFlow(),
            idOf = { it.msgId },
        ) { cmd, _ ->
            TestEntity(cmd.id, TestState.Active) to DoneEvt(cmd.id)
        }.toList()

        assertEquals(listOf("msg-1", "msg-2", "msg-3"), outcomes.map { it.msgId })
    }

    @Test
    fun `idOf is decoupled from cmd id - independently controllable`() = runTest {
        val evolver = makeEvolver()
        val cmds = listOf(CreateCmd(id = "ent-A", msgId = "msg-1"))

        // idOf returns "FORCED" — proving the framework respects the extractor and does NOT
        // fall back to cmd.id or a random UUID.
        val outcomes = evolver.evolveWithOutcomes(
            commands = cmds.asFlow(),
            idOf = { "FORCED" },
        ) { cmd ->
            TestEntity(cmd.id, TestState.Created) to CreatedEvt(cmd.id)
        }.toList()

        assertEquals(listOf("FORCED"), outcomes.map { it.msgId })
    }
}
