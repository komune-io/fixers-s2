package s2.automate.core.storing

import f2.dsl.cqrs.enveloped.EnvelopedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import s2.automate.core.persist.PersistOutcome
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.sourcing.dsl.Decide
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the clearer aliases added on [S2AutomateStoringEvolverFlow] to the legacy `evolve*`
 * methods they replace: an implementation that only overrides the deprecated `evolve*` methods
 * must keep working when callers migrate to `create`/`transition`.
 */
@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class S2AutomateStoringEvolverFlowAliasTest {

    enum class TestState(override val position: Int) : S2State {
        Created(0)
    }

    data class TestEntity(val id: String) : WithS2Id<String>, WithS2State<TestState> {
        override fun s2Id() = id
        override fun s2State() = TestState.Created
    }

    data class CreateCmd(val id: String) : S2InitCommand
    data class DoCmd(override val id: String) : S2Command<String>
    data class TestEvt(val entityId: String) : Evt

    /**
     * Legacy-shaped implementation: only the deprecated `evolve*` methods are overridden,
     * exactly like a consumer written before the aliases existed.
     */
    private class RecordingEvolverFlow :
        S2AutomateStoringEvolverFlow<TestState, String, TestEntity, Evt> {

        val calls = mutableListOf<String>()

        override suspend fun <COMMAND : S2InitCommand, EVENT_OUT : Evt> evolve(
            commands: Flow<COMMAND>,
            build: S2EvolveInitFnc<COMMAND, TestEntity, EVENT_OUT>
        ): Flow<EVENT_OUT> {
            calls.add("evolve(init-flow)")
            return emptyFlow()
        }

        override suspend fun <COMMAND : S2InitCommand, EVENT_OUT : Evt> evolveEnvelope(
            commands: EnvelopedFlow<COMMAND>,
            build: S2EvolveInitFnc<COMMAND, TestEntity, EVENT_OUT>
        ): EnvelopedFlow<EVENT_OUT> {
            calls.add("evolveEnvelope(init-flow)")
            return emptyFlow()
        }

        override fun <COMMAND : S2InitCommand, EVENT_OUT : Evt> evolve(
            build: S2EvolveInitFnc<COMMAND, TestEntity, EVENT_OUT>
        ): Decide<COMMAND, EVENT_OUT> {
            calls.add("evolve(init-decide)")
            return Decide { emptyFlow() }
        }

        override suspend fun <COMMAND : S2Command<String>, EVENT_OUT : Evt> evolve(
            commands: Flow<COMMAND>,
            exec: S2EvolveFnc<COMMAND, TestEntity, EVENT_OUT>
        ): Flow<EVENT_OUT> {
            calls.add("evolve(transition-flow)")
            return emptyFlow()
        }

        override suspend fun <COMMAND : S2Command<String>, EVENT_OUT : Evt> evolveEnvelope(
            commands: EnvelopedFlow<COMMAND>,
            exec: S2EvolveFnc<COMMAND, TestEntity, EVENT_OUT>
        ): EnvelopedFlow<EVENT_OUT> {
            calls.add("evolveEnvelope(transition-flow)")
            return emptyFlow()
        }

        override fun <COMMAND : S2Command<String>, EVENT_OUT : Evt> evolve(
            fnc: S2EvolveFnc<COMMAND, TestEntity, EVENT_OUT>
        ): Decide<COMMAND, EVENT_OUT> {
            calls.add("evolve(transition-decide)")
            return Decide { emptyFlow() }
        }

        override suspend fun <COMMAND : S2InitCommand, EVENT_OUT : Evt> evolveWithOutcomes(
            commands: Flow<COMMAND>,
            idOf: (COMMAND) -> String,
            build: S2EvolveInitFnc<COMMAND, TestEntity, EVENT_OUT>
        ): Flow<PersistOutcome<EVENT_OUT>> {
            calls.add("evolveWithOutcomes(init-flow)")
            return emptyFlow()
        }

        override suspend fun <COMMAND : S2Command<String>, EVENT_OUT : Evt> evolveWithOutcomes(
            commands: Flow<COMMAND>,
            idOf: (COMMAND) -> String,
            exec: S2EvolveFnc<COMMAND, TestEntity, EVENT_OUT>
        ): Flow<PersistOutcome<EVENT_OUT>> {
            calls.add("evolveWithOutcomes(transition-flow)")
            return emptyFlow()
        }
    }

    private val initBuild: S2EvolveInitFnc<CreateCmd, TestEntity, TestEvt> =
        { cmd -> TestEntity(cmd.id) to TestEvt(cmd.id) }
    private val transitionExec: S2EvolveFnc<DoCmd, TestEntity, TestEvt> =
        { cmd, entity -> entity to TestEvt(cmd.id) }

    @Test
    fun `create delegates to the deprecated init evolve`() = runTest {
        val evolver = RecordingEvolverFlow()
        evolver.create(emptyFlow<CreateCmd>(), initBuild)
        assertEquals(listOf("evolve(init-flow)"), evolver.calls)
    }

    @Test
    fun `createEnvelope delegates to the deprecated init evolveEnvelope`() = runTest {
        val evolver = RecordingEvolverFlow()
        evolver.createEnvelope(emptyFlow(), initBuild)
        assertEquals(listOf("evolveEnvelope(init-flow)"), evolver.calls)
    }

    @Test
    fun `decideCreate delegates to the deprecated init evolve decider`() = runTest {
        val evolver = RecordingEvolverFlow()
        evolver.decideCreate(initBuild)
        assertEquals(listOf("evolve(init-decide)"), evolver.calls)
    }

    @Test
    fun `transition delegates to the deprecated transition evolve`() = runTest {
        val evolver = RecordingEvolverFlow()
        evolver.transition(emptyFlow<DoCmd>(), transitionExec)
        assertEquals(listOf("evolve(transition-flow)"), evolver.calls)
    }

    @Test
    fun `transitionEnvelope delegates to the deprecated transition evolveEnvelope`() = runTest {
        val evolver = RecordingEvolverFlow()
        evolver.transitionEnvelope(emptyFlow(), transitionExec)
        assertEquals(listOf("evolveEnvelope(transition-flow)"), evolver.calls)
    }

    @Test
    fun `decideTransition delegates to the deprecated transition evolve decider`() = runTest {
        val evolver = RecordingEvolverFlow()
        evolver.decideTransition(transitionExec)
        assertEquals(listOf("evolve(transition-decide)"), evolver.calls)
    }

    @Test
    fun `createWithOutcomes delegates to the deprecated init evolveWithOutcomes`() = runTest {
        val evolver = RecordingEvolverFlow()
        evolver.createWithOutcomes(emptyFlow<CreateCmd>(), { it.id }, initBuild)
        assertEquals(listOf("evolveWithOutcomes(init-flow)"), evolver.calls)
    }

    @Test
    fun `transitionWithOutcomes delegates to the deprecated transition evolveWithOutcomes`() = runTest {
        val evolver = RecordingEvolverFlow()
        evolver.transitionWithOutcomes(emptyFlow<DoCmd>(), { it.id }, transitionExec)
        assertEquals(listOf("evolveWithOutcomes(transition-flow)"), evolver.calls)
    }
}
