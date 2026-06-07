package s2.spring.automate.executor

import f2.dsl.cqrs.envelope.Envelope
import f2.dsl.cqrs.enveloped.EnvelopedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import s2.automate.core.appevent.listener.AutomateListenerAdapter
import s2.automate.core.appevent.publisher.AppEventPublisher
import s2.dsl.automate.S2Automate
import s2.automate.core.engine.S2AutomateEngine
import s2.automate.core.engine.S2AutomateOutcomeEngine
import s2.automate.core.persist.PersistOutcome
import s2.automate.core.storing.S2AutomateStoringEvolverImpl
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

/**
 * Verifies that S2AutomateExecutorSpring.evolveWithOutcomes (both overloads) are pure
 * delegations to engine.evolveWithOutcomes — no wrapping, no filtering, exact identity.
 */
class S2AutomateExecutorSpringPassthroughTest {

    // ---- domain fixtures ----

    enum class TestState(override var position: Int) : S2State {
        Created(0)
    }

    data class TestEntity(val id: String) :
        WithS2Id<String>, WithS2State<TestState> {
        override fun s2Id() = id
        override fun s2State() = TestState.Created
    }

    data class CreateCmd(val id: String) : S2InitCommand
    data class DoCmd(override val id: String) : S2Command<String>
    data class TestEvt(val id: String) : Evt

    // ---- sentinel flows ----

    private val sentinelInitFlow: Flow<PersistOutcome<TestEvt>> = flowOf(
        PersistOutcome.Success(msgId = "cmd-init", event = TestEvt("sentinel-init"))
    )

    private val sentinelTransFlow: Flow<PersistOutcome<TestEvt>> = flowOf(
        PersistOutcome.Success(msgId = "cmd-trans", event = TestEvt("sentinel-trans"))
    )

    // ---- no-op legacy engine (never called in this test) ----

    private object NoOpLegacyEngine : S2AutomateEngine<TestState, TestEntity, String, Evt> {
        override suspend fun <COMMAND : S2InitCommand, ENTITY_OUT : TestEntity, EVENT_OUT : Evt> create(
            commands: EnvelopedFlow<COMMAND>,
            decide: suspend (cmd: Envelope<COMMAND>) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<EVENT_OUT> = error("should not be called")

        override suspend fun <COMMAND : S2Command<String>, ENTITY_OUT : TestEntity, EVENT_OUT : Evt> doTransition(
            commands: EnvelopedFlow<COMMAND>,
            exec: suspend (Envelope<out COMMAND>, TestEntity) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<EVENT_OUT> = error("should not be called")
    }

    // ---- no-op outcome engine (never called in this test) ----

    private object NoOpOutcomeEngine : S2AutomateOutcomeEngine<TestState, TestEntity, String, Evt> {
        override suspend fun <COMMAND : S2InitCommand, ENTITY_OUT : TestEntity, EVENT_OUT : Evt> createWithOutcomes(
            commands: EnvelopedFlow<COMMAND>,
            decide: suspend (cmd: Envelope<COMMAND>) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<PersistOutcome<EVENT_OUT>> = error("should not be called")

        override suspend fun <
            COMMAND : S2Command<String>,
            ENTITY_OUT : TestEntity,
            EVENT_OUT : Evt,
        > doTransitionWithOutcomes(
            commands: EnvelopedFlow<COMMAND>,
            exec: suspend (Envelope<out COMMAND>, TestEntity) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<PersistOutcome<EVENT_OUT>> = error("should not be called")
    }

    private object NoOpPublisher : AppEventPublisher {
        override fun <EVENT> publish(event: EVENT & Any) = Unit
    }

    // ---- stub evolver ----

    /**
     * Subclass of S2AutomateStoringEvolverImpl that overrides both evolveWithOutcomes
     * overloads to return the pre-configured sentinel flows.
     */
    private inner class StubEvolver : S2AutomateStoringEvolverImpl<TestState, TestEntity, String>(
        automateExecutor = NoOpLegacyEngine,
        outcomeExecutor = NoOpOutcomeEngine,
        publisher = NoOpPublisher,
        listener = AutomateListenerAdapter<TestState, String, TestEntity, S2Automate>(),
    ) {
        override suspend fun <COMMAND : S2InitCommand, EVENT_OUT : Evt> evolveWithOutcomes(
            commands: Flow<COMMAND>,
            idOf: (COMMAND) -> String,
            build: suspend (cmd: COMMAND) -> Pair<TestEntity, EVENT_OUT>
        ): Flow<PersistOutcome<EVENT_OUT>> {
            @Suppress("UNCHECKED_CAST")
            return sentinelInitFlow as Flow<PersistOutcome<EVENT_OUT>>
        }

        override suspend fun <COMMAND : S2Command<String>, EVENT_OUT : Evt> evolveWithOutcomes(
            commands: Flow<COMMAND>,
            idOf: (COMMAND) -> String,
            exec: suspend (COMMAND, TestEntity) -> Pair<TestEntity, EVENT_OUT>
        ): Flow<PersistOutcome<EVENT_OUT>> {
            @Suppress("UNCHECKED_CAST")
            return sentinelTransFlow as Flow<PersistOutcome<EVENT_OUT>>
        }
    }

    // ---- helper: create executor with stub engine injected via reflection ----

    private fun makeExecutor(): S2AutomateExecutorSpring<TestState, String, TestEntity> {
        val executor = object : S2AutomateExecutorSpring<TestState, String, TestEntity>() {}
        val stub = StubEvolver()
        // Inject stub via reflection since 'engine' is protected lateinit
        S2AutomateExecutorSpring::class.java
            .getDeclaredField("engine")
            .apply { isAccessible = true }
            .set(executor, stub)
        return executor
    }

    // ---- tests ----

    @Test
    fun `evolveWithOutcomes (init) delegates to engine evolveWithOutcomes (init)`() = runTest {
        val executor = makeExecutor()

        val returned: Flow<PersistOutcome<TestEvt>> = executor.evolveWithOutcomes(
            commands = flowOf(CreateCmd("id1")),
            idOf = { it.id },
            build = { cmd: CreateCmd -> TestEntity(cmd.id) to TestEvt(cmd.id) }
        )

        // identity check: returned flow must be the sentinel flow
        assertThat(returned).isSameAs(sentinelInitFlow)
    }

    @Test
    fun `evolveWithOutcomes (transition) delegates to engine evolveWithOutcomes (transition)`() = runTest {
        val executor = makeExecutor()

        val returned: Flow<PersistOutcome<TestEvt>> = executor.evolveWithOutcomes(
            commands = flowOf(DoCmd("id1")),
            idOf = { it.id },
            exec = { cmd: DoCmd, _: TestEntity -> TestEntity(cmd.id) to TestEvt(cmd.id) }
        )

        // identity check: returned flow must be the sentinel flow
        assertThat(returned).isSameAs(sentinelTransFlow)
    }
}
