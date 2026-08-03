package s2.spring.automate.executor

import f2.dsl.cqrs.envelope.Envelope
import f2.dsl.cqrs.envelope.asEnvelopeWithType
import f2.dsl.cqrs.enveloped.EnvelopedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import s2.automate.core.appevent.listener.AutomateListenerAdapter
import s2.automate.core.appevent.publisher.AppEventPublisher
import s2.automate.core.engine.S2AutomateEngine
import s2.automate.core.engine.S2AutomateOutcomeEngine
import s2.automate.core.persist.PersistOutcome
import s2.automate.core.storing.S2AutomateStoringEvolverImpl
import s2.automate.core.storing.S2EvolveFnc
import s2.automate.core.storing.S2EvolveInitFnc
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.sourcing.dsl.Decide

/**
 * Verifies every non-outcome method of [S2AutomateExecutorSpring] is a pure delegation
 * to the injected engine (identity of returned value / event).
 */
class S2AutomateExecutorSpringDelegationTest {

    enum class TestState(override var position: Int) : S2State { Created(0) }

    data class TestEntity(val id: String) : WithS2Id<String>, WithS2State<TestState> {
        override fun s2Id() = id
        override fun s2State() = TestState.Created
    }

    data class CreateCmd(val id: String) : S2InitCommand
    data class DoCmd(override val id: String) : S2Command<String>
    data class TestEvt(val id: String) : Evt

    // ---- sentinels ----

    private val sentinelCreate = TestEvt("create-3arg")
    private val sentinelCreateBuild = TestEvt("create-build")
    private val sentinelTransition = TestEvt("transition")
    private val sentinelEvolveInitFlow: Flow<TestEvt> = flowOf(TestEvt("evolve-init-flow"))
    private val sentinelEvolveTransFlow: Flow<TestEvt> = flowOf(TestEvt("evolve-trans-flow"))
    private val sentinelDecideInit: Decide<CreateCmd, TestEvt> = Decide { flowOf() }
    private val sentinelDecideTrans: Decide<DoCmd, TestEvt> = Decide { flowOf() }
    private val sentinelEnvInit: EnvelopedFlow<TestEvt> = flowOf(TestEvt("env-init").asEnvelopeWithType(type = "Evt"))
    private val sentinelEnvTrans: EnvelopedFlow<TestEvt> = flowOf(TestEvt("env-trans").asEnvelopeWithType(type = "Evt"))

    // ---- no-op engines (never driven) ----

    private object NoOpLegacyEngine : S2AutomateEngine<TestState, TestEntity, String, Evt> {
        override fun <COMMAND : S2InitCommand, ENTITY_OUT : TestEntity, EVENT_OUT : Evt> create(
            commands: EnvelopedFlow<COMMAND>,
            decide: suspend (cmd: Envelope<COMMAND>) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<EVENT_OUT> = error("should not be called")

        override fun <COMMAND : S2Command<String>, ENTITY_OUT : TestEntity, EVENT_OUT : Evt> doTransition(
            commands: EnvelopedFlow<COMMAND>,
            exec: suspend (Envelope<out COMMAND>, TestEntity) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<EVENT_OUT> = error("should not be called")
    }

    private object NoOpOutcomeEngine : S2AutomateOutcomeEngine<TestState, TestEntity, String, Evt> {
        override fun <COMMAND : S2InitCommand, ENTITY_OUT : TestEntity, EVENT_OUT : Evt> createWithOutcomes(
            commands: EnvelopedFlow<COMMAND>,
            decide: suspend (cmd: Envelope<COMMAND>) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<PersistOutcome<EVENT_OUT>> = error("should not be called")

        override fun <COMMAND : S2Command<String>, ENTITY_OUT : TestEntity, EVENT_OUT : Evt> doTransitionWithOutcomes(
            commands: EnvelopedFlow<COMMAND>,
            exec: suspend (Envelope<out COMMAND>, TestEntity) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<PersistOutcome<EVENT_OUT>> = error("should not be called")
    }

    private object NoOpPublisher : AppEventPublisher {
        override fun <EVENT> publish(event: EVENT & Any) = Unit
    }

    // ---- stub evolver returning sentinels for every delegated method ----

    private inner class StubEvolver : S2AutomateStoringEvolverImpl<TestState, TestEntity, String>(
        automateExecutor = NoOpLegacyEngine,
        outcomeExecutor = NoOpOutcomeEngine,
        publisher = NoOpPublisher,
        listener = AutomateListenerAdapter<TestState, String, TestEntity, S2Automate>(),
    ) {
        @Suppress("UNCHECKED_CAST")
        override suspend fun <EVENT_OUT : Evt> createWithEvent(
            command: S2InitCommand,
            buildEvent: suspend TestEntity.() -> EVENT_OUT,
            buildEntity: suspend () -> TestEntity,
        ): EVENT_OUT = sentinelCreate as EVENT_OUT

        @Suppress("UNCHECKED_CAST")
        override suspend fun <EVENT_OUT : Evt> createWithEvent(
            command: S2InitCommand,
            build: suspend () -> Pair<TestEntity, EVENT_OUT>,
        ): EVENT_OUT = sentinelCreateBuild as EVENT_OUT

        @Suppress("UNCHECKED_CAST")
        override suspend fun <EVENT_OUT : Evt> doTransition(
            command: S2Command<String>,
            exec: suspend TestEntity.() -> Pair<TestEntity, EVENT_OUT>,
        ): EVENT_OUT = sentinelTransition as EVENT_OUT

        @Suppress("UNCHECKED_CAST")
        override fun <COMMAND : S2InitCommand, EVENT_OUT : Evt> evolve(
            commands: Flow<COMMAND>,
            build: suspend (cmd: COMMAND) -> Pair<TestEntity, EVENT_OUT>
        ): Flow<EVENT_OUT> = sentinelEvolveInitFlow as Flow<EVENT_OUT>

        @Suppress("UNCHECKED_CAST")
        override fun <COMMAND : S2Command<String>, EVENT_OUT : Evt> evolve(
            commands: Flow<COMMAND>,
            exec: suspend (COMMAND, TestEntity) -> Pair<TestEntity, EVENT_OUT>
        ): Flow<EVENT_OUT> = sentinelEvolveTransFlow as Flow<EVENT_OUT>

        @Suppress("UNCHECKED_CAST")
        override fun <COMMAND : S2Command<String>, EVENT_OUT : Evt> evolve(
            fnc: suspend (COMMAND, TestEntity) -> Pair<TestEntity, EVENT_OUT>
        ): Decide<COMMAND, EVENT_OUT> = sentinelDecideTrans as Decide<COMMAND, EVENT_OUT>

        @Suppress("UNCHECKED_CAST")
        override fun <COMMAND : S2InitCommand, EVENT_OUT : Evt> evolve(
            build: suspend (cmd: COMMAND) -> Pair<TestEntity, EVENT_OUT>
        ): Decide<COMMAND, EVENT_OUT> = sentinelDecideInit as Decide<COMMAND, EVENT_OUT>

        @Suppress("UNCHECKED_CAST")
        override fun <COMMAND : S2InitCommand, EVENT_OUT : Evt> evolveEnvelope(
            commands: EnvelopedFlow<COMMAND>,
            build: S2EvolveInitFnc<COMMAND, TestEntity, EVENT_OUT>
        ): EnvelopedFlow<EVENT_OUT> = sentinelEnvInit as EnvelopedFlow<EVENT_OUT>

        @Suppress("UNCHECKED_CAST")
        override fun <COMMAND : S2Command<String>, EVENT_OUT : Evt> evolveEnvelope(
            commands: EnvelopedFlow<COMMAND>,
            exec: S2EvolveFnc<COMMAND, TestEntity, EVENT_OUT>
        ): EnvelopedFlow<EVENT_OUT> = sentinelEnvTrans as EnvelopedFlow<EVENT_OUT>
    }

    private fun makeExecutor(): S2AutomateExecutorSpring<TestState, String, TestEntity> {
        val executor = object : S2AutomateExecutorSpring<TestState, String, TestEntity>() {}
        S2AutomateExecutorSpring::class.java
            .getDeclaredField("engine")
            .apply { isAccessible = true }
            .set(executor, StubEvolver())
        return executor
    }

    // ---- tests ----

    @Test
    suspend fun `createWithEvent (buildEvent, buildEntity) delegates`() {
        val result = makeExecutor().createWithEvent(
            command = CreateCmd("id"),
            buildEvent = { TestEvt(id) },
            buildEntity = { TestEntity("id") },
        )
        assertThat(result).isSameAs(sentinelCreate)
    }

    @Test
    suspend fun `createWithEvent (build) delegates`() {
        val result = makeExecutor().createWithEvent(
            command = CreateCmd("id"),
            build = { TestEntity("id") to TestEvt("id") },
        )
        assertThat(result).isSameAs(sentinelCreateBuild)
    }

    @Test
    suspend fun `doTransition delegates`() {
        val result = makeExecutor().doTransition(
            command = DoCmd("id"),
            exec = { this to TestEvt(id) },
        )
        assertThat(result).isSameAs(sentinelTransition)
    }

    @Test
    suspend fun `evolve (init flow) delegates`() {
        val result = makeExecutor().evolve(
            commands = flowOf(CreateCmd("id")),
            build = { cmd: CreateCmd -> TestEntity(cmd.id) to TestEvt(cmd.id) },
        )
        assertThat(result).isSameAs(sentinelEvolveInitFlow)
    }

    @Test
    suspend fun `evolve (transition flow) delegates`() {
        val result = makeExecutor().evolve(
            commands = flowOf(DoCmd("id")),
            exec = { cmd: DoCmd, _: TestEntity -> TestEntity(cmd.id) to TestEvt(cmd.id) },
        )
        assertThat(result).isSameAs(sentinelEvolveTransFlow)
    }

    @Test
    suspend fun `evolve (transition Decide) delegates`() {
        val result = makeExecutor().evolve<DoCmd, TestEvt>(
            fnc = { cmd, _ -> TestEntity(cmd.id) to TestEvt(cmd.id) },
        )
        assertThat(result).isSameAs(sentinelDecideTrans)
    }

    @Test
    suspend fun `evolve (init Decide) delegates`() {
        val result = makeExecutor().evolve<CreateCmd, TestEvt>(
            build = { cmd -> TestEntity(cmd.id) to TestEvt(cmd.id) },
        )
        assertThat(result).isSameAs(sentinelDecideInit)
    }

    @Test
    suspend fun `evolveEnvelope (init) delegates`() {
        val result = makeExecutor().evolveEnvelope(
            commands = flowOf(CreateCmd("id").asEnvelopeWithType(type = "Cmd")),
            build = { cmd: CreateCmd -> TestEntity(cmd.id) to TestEvt(cmd.id) },
        )
        assertThat(result).isSameAs(sentinelEnvInit)
    }

    @Test
    suspend fun `evolveEnvelope (transition) delegates`() {
        val result = makeExecutor().evolveEnvelope(
            commands = flowOf(DoCmd("id").asEnvelopeWithType(type = "Cmd")),
            exec = { cmd: DoCmd, _: TestEntity -> TestEntity(cmd.id) to TestEvt(cmd.id) },
        )
        assertThat(result).isSameAs(sentinelEnvTrans)
    }
}
