package s2.automate.core.persist

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import s2.automate.core.config.S2BatchProperties
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.TransitionAppliedContext
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.builder.s2
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AutomatePersisterDefaultsTest {

	private val testAutomate: S2Automate = s2 { name = "TestAutomate" }
	private val automateContext = AutomateContext(testAutomate, S2BatchProperties())

	@Test
	fun `default persistWithOutcomes wraps each event in Success with msgId from context`() = runTest {
		// One event per context; StubLegacyPersister emits the single-element list per call.
		val persister = StubLegacyPersister(eventsToEmit = listOf("singleton"))

		val ctx1 = makeTransitionCtx("m1", "e1")
		val ctx2 = makeTransitionCtx("m2", "e2")
		val ctx3 = makeTransitionCtx("m3", "e3")

		val outcomes: List<PersistOutcome<String>> = persister
			.persistWithOutcomes(flowOf(ctx1, ctx2, ctx3))
			.toList()

		assertEquals(3, outcomes.size)
		assertTrue(outcomes.all { it is PersistOutcome.Success<String> })
		assertEquals(listOf("m1", "m2", "m3"), outcomes.map { it.msgId })
	}

	@Test
	fun `default persistInitWithOutcomes wraps each event in Success with msgId from context`() = runTest {
		val persister = StubLegacyPersister(eventsToEmit = listOf("singleton"))

		val ctx1 = makeInitCtx("m1", "init1")
		val ctx2 = makeInitCtx("m2", "init2")

		val outcomes: List<PersistOutcome<String>> = persister
			.persistInitWithOutcomes(flowOf(ctx1, ctx2))
			.toList()

		assertEquals(2, outcomes.size)
		assertTrue(outcomes.all { it is PersistOutcome.Success<String> })
		assertEquals(listOf("m1", "m2"), outcomes.map { it.msgId })
	}

	// ---- fixtures ----

	enum class TestState(override var position: Int) : S2State {
		Created(0)
	}

	data class TestEntity(val id: String) : WithS2Id<String>, WithS2State<TestState> {
		override fun s2Id(): String = id
		override fun s2State(): TestState = TestState.Created
	}

	private data class StubCmd(override val id: String) : S2Command<String>

	private data class StubInitCmd(val id: String) : S2InitCommand

	private fun makeTransitionCtx(msgId: String, entityId: String) =
		TransitionAppliedContext<TestState, String, TestEntity, String, S2Automate>(
			automateContext = automateContext,
			msgId = msgId,
			from = TestState.Created,
			msg = StubCmd(entityId),
			event = entityId,
			entity = TestEntity(entityId),
		)

	private fun makeInitCtx(msgId: String, entityId: String) =
		InitTransitionAppliedContext<TestState, String, TestEntity, String, S2Automate>(
			automateContext = automateContext,
			msgId = msgId,
			msg = StubInitCmd(entityId),
			event = entityId,
			entity = TestEntity(entityId),
		)

	/**
	 * Implements AutomatePersister but only the legacy `persist` / `persistInit` methods.
	 * The `*WithOutcomes` variants are intentionally NOT overridden so tests exercise
	 * the interface's default implementations that wrap each event into PersistOutcome.Success.
	 *
	 * For each context it receives, it emits the single event from [eventsToEmit].
	 * Callers must ensure [eventsToEmit] has exactly one element so the 1-context → 1-outcome
	 * invariant holds.
	 */
	private class StubLegacyPersister(
		private val eventsToEmit: List<String>,
	) : AutomatePersister<TestState, String, TestEntity, String, S2Automate> {

		override suspend fun persistInit(
			transitionContexts: Flow<InitTransitionAppliedContext<TestState, String, TestEntity, String, S2Automate>>
		): Flow<String> = flowOf(*eventsToEmit.toTypedArray())

		override suspend fun persist(
			transitionContexts: Flow<TransitionAppliedContext<TestState, String, TestEntity, String, S2Automate>>
		): Flow<String> = flowOf(*eventsToEmit.toTypedArray())

		override suspend fun load(
			automateContexts: AutomateContext<S2Automate>,
			ids: Flow<String>,
		): Flow<TestEntity?> = flowOf()

		override suspend fun load(
			automateContexts: AutomateContext<S2Automate>,
			id: String,
		): TestEntity? = null
	}
}
