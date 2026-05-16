package s2.automate.core.persist

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.TransitionAppliedContext
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AutomatePersisterDefaultsTest {

	@Test
	fun `default persistWithOutcomes wraps each event in Success with empty commandId`() = runTest {
		val persister = StubLegacyPersister(eventsToEmit = listOf("e1", "e2", "e3"))

		val outcomes: List<PersistOutcome<String>> = persister
			.persistWithOutcomes(flowOf())
			.toList()

		assertEquals(3, outcomes.size)
		assertTrue(outcomes.all { it is PersistOutcome.Success<String> })
		val events = outcomes.filterIsInstance<PersistOutcome.Success<String>>().map { it.event }
		assertEquals(listOf("e1", "e2", "e3"), events)
		assertTrue(outcomes.all { it.commandId.isEmpty() })
	}

	@Test
	fun `default persistInitWithOutcomes wraps each event in Success with empty commandId`() = runTest {
		val persister = StubLegacyPersister(eventsToEmit = listOf("init1", "init2"))

		val outcomes: List<PersistOutcome<String>> = persister
			.persistInitWithOutcomes(flowOf())
			.toList()

		assertEquals(2, outcomes.size)
		assertTrue(outcomes.all { it is PersistOutcome.Success<String> })
	}

	// ---- fixtures ----

	enum class TestState(override var position: Int) : S2State {
		Created(0)
	}

	data class TestEntity(val id: String) : WithS2Id<String>, WithS2State<TestState> {
		override fun s2Id(): String = id
		override fun s2State(): TestState = TestState.Created
	}

	/**
	 * Implements AutomatePersister but only the legacy `persist` / `persistInit` methods.
	 * The `*WithOutcomes` variants are intentionally NOT overridden so tests exercise
	 * the interface's default implementations that wrap each event into PersistOutcome.Success.
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
