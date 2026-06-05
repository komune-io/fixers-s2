package s2.automate.core.persist

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.config.S2BatchProperties
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2Role
import s2.dsl.automate.S2State
import s2.dsl.automate.builder.s2
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Verifies the default implementation of [AutomatePersister.loadWithOutcomes]
 * (defined directly on the interface) classifies each id correctly given the
 * legacy [AutomatePersister.load] return value:
 *  - emitted entity → [LoadOutcome.Loaded]
 *  - absent / null-emitted id → [LoadOutcome.Rejected] (ERROR_ENTITY_NOT_FOUND)
 *  - legacy load throws → every id becomes [LoadOutcome.Transient]
 *
 * Implementations may override the default; this test pins the contract
 * provided by the interface.
 */
class AutomatePersisterLoadOutcomeDefaultsTest {

    // ---- domain fixtures ----

    private enum class TestState(override var position: Int) : S2State {
        Created(0), Active(1)
    }

    private object TestRole : S2Role

    private data class TestEntity(
        val id: String,
        val state: TestState,
    ) : WithS2Id<String>, WithS2State<TestState> {
        override fun s2Id(): String = id
        override fun s2State(): TestState = state
    }

    private sealed interface TestEvent { val entityId: String }

    private val testAutomate: S2Automate = s2 {
        name = "TestAutomate"
        init<s2.dsl.automate.S2InitCommand> {
            to = TestState.Created
            role = TestRole
        }
    }

    private val automateContext = AutomateContext(testAutomate, S2BatchProperties())

    // ---- stub persisters that only define the legacy load overloads,
    //      so the interface's default loadWithOutcomes is exercised ----

    /**
     * Emits an entity for every id (no nulls). Used to assert the default
     * impl wraps every id as [LoadOutcome.Loaded].
     */
    private class FullPersister :
        AutomatePersister<TestState, String, TestEntity, TestEvent, S2Automate> {

        override suspend fun persistInit(
            transitionContexts: Flow<InitTransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<TestEvent> = error("not used")

        override suspend fun persist(
            transitionContexts: Flow<TransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<TestEvent> = error("not used")

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            ids: Flow<String>,
        ): Flow<TestEntity?> = ids.map { id -> TestEntity(id, TestState.Created) }

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            id: String,
        ): TestEntity? = TestEntity(id, TestState.Created)
    }

    /**
     * Emits nothing — every requested id is "not found". Default impl should
     * surface them as [LoadOutcome.Rejected] with ERROR_ENTITY_NOT_FOUND.
     */
    private class EmptyPersister :
        AutomatePersister<TestState, String, TestEntity, TestEvent, S2Automate> {

        override suspend fun persistInit(
            transitionContexts: Flow<InitTransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<TestEvent> = error("not used")

        override suspend fun persist(
            transitionContexts: Flow<TransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<TestEvent> = error("not used")

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            ids: Flow<String>,
        ): Flow<TestEntity?> = ids.map { null }

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            id: String,
        ): TestEntity? = null
    }

    /**
     * Emits an entity for some ids and nothing for others (simulates a backing
     * store that returns a partial result set).
     */
    private class PartialPersister(private val found: Set<String>) :
        AutomatePersister<TestState, String, TestEntity, TestEvent, S2Automate> {

        override suspend fun persistInit(
            transitionContexts: Flow<InitTransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<TestEvent> = error("not used")

        override suspend fun persist(
            transitionContexts: Flow<TransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<TestEvent> = error("not used")

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            ids: Flow<String>,
        ): Flow<TestEntity?> = ids.map { id ->
            if (id in found) TestEntity(id, TestState.Created) else null
        }

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            id: String,
        ): TestEntity? = if (id in found) TestEntity(id, TestState.Created) else null
    }

    /**
     * Legacy load() throws — exercises the default impl's runCatching branch.
     */
    private class ThrowingPersister(private val cause: Throwable) :
        AutomatePersister<TestState, String, TestEntity, TestEvent, S2Automate> {

        override suspend fun persistInit(
            transitionContexts: Flow<InitTransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<TestEvent> = error("not used")

        override suspend fun persist(
            transitionContexts: Flow<TransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<TestEvent> = error("not used")

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            ids: Flow<String>,
        ): Flow<TestEntity?> = kotlinx.coroutines.flow.flow { throw cause }

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            id: String,
        ): TestEntity? = throw cause
    }

    // ---- tests ----

    @Test
    fun `default loadWithOutcomes emits Loaded for every id when load returns all entities`() = runTest {
        val persister = FullPersister()
        val ids = listOf("a", "b", "c")

        val outcomes = persister.loadWithOutcomes(automateContext, ids.asFlow()).toList()

        assertEquals(ids.size, outcomes.size)
        outcomes.forEachIndexed { i, outcome ->
            assertIs<LoadOutcome.Loaded<String, TestEntity>>(outcome,
                "Expected Loaded for id=${ids[i]} but got ${outcome::class.simpleName}")
            assertEquals(ids[i], outcome.id, "Loaded.id must match input id")
            assertEquals(ids[i], outcome.entity.id, "Loaded.entity.id must match")
        }
    }

    @Test
    fun `default loadWithOutcomes emits Rejected ERROR_ENTITY_NOT_FOUND for ids missing from load`() = runTest {
        val persister = PartialPersister(found = setOf("a", "c"))
        val ids = listOf("a", "b", "c")

        val outcomes = persister.loadWithOutcomes(automateContext, ids.asFlow()).toList()

        assertEquals(3, outcomes.size, "must emit one outcome per requested id")
        val byId = outcomes.associateBy { it.id }

        assertIs<LoadOutcome.Loaded<String, TestEntity>>(byId.getValue("a"))
        assertIs<LoadOutcome.Loaded<String, TestEntity>>(byId.getValue("c"))

        val b = assertIs<LoadOutcome.Rejected<String, TestEntity>>(byId.getValue("b"),
            "Missing id must be Rejected, got ${byId.getValue("b")::class.simpleName}")
        assertEquals("ERROR_ENTITY_NOT_FOUND", b.error.type)
        assertEquals(ErrorCategory.Rejected, b.category,
            "Rejected variant must carry ErrorCategory.Rejected for retry-policy consumers")
        assertTrue("b" in b.error.description, "Rejected description should name the offender")
    }

    @Test
    fun `default loadWithOutcomes emits Transient ERROR_PERSIST_LAMBDA_THROW when legacy load throws`() = runTest {
        val cause = RuntimeException("backing store unreachable")
        val persister = ThrowingPersister(cause)
        val ids = listOf("a", "b", "c")

        val outcomes = persister.loadWithOutcomes(automateContext, ids.asFlow()).toList()

        // When the legacy load throws, we can't classify per id — every id becomes Transient
        // so the consumer can retry the whole batch (typical for network/timeout failures).
        assertEquals(ids.size, outcomes.size, "every id must surface as a Transient on legacy-load throw")
        outcomes.forEach { outcome ->
            val transient = assertIs<LoadOutcome.Transient<String, TestEntity>>(outcome,
                "Expected Transient but got ${outcome::class.simpleName}")
            assertEquals("ERROR_PERSIST_LAMBDA_THROW", transient.error.type)
            assertEquals(ErrorCategory.Transient, transient.category)
            assertNotNull(transient.error.cause, "Transient should preserve the underlying throwable")
            assertEquals(cause, transient.error.cause)
        }
        assertEquals(ids.toSet(), outcomes.map { it.id }.toSet(),
            "every input id must be represented in the Transient outcomes")
    }

    @Test
    fun `default loadWithOutcomes preserves input id type and order`() = runTest {
        val persister = FullPersister()
        val ids = listOf("z", "x", "y")  // intentionally not sorted

        val outcomes = persister.loadWithOutcomes(automateContext, ids.asFlow()).toList()

        assertEquals(ids, outcomes.map { it.id },
            "default impl must emit outcomes in input order to keep correlation predictable")
    }

    @Test
    fun `default loadWithOutcomes handles empty input`() = runTest {
        val persister = FullPersister()

        val outcomes = persister.loadWithOutcomes(automateContext, flowOf<String>()).toList()

        assertTrue(outcomes.isEmpty(), "empty ids in → empty outcomes out")
    }

    @Test
    fun `default loadWithOutcomes propagates CancellationException instead of converting to Transient`() = runTest {
        // Cooperative cancellation MUST propagate up the coroutine tree. If we
        // accidentally caught it (e.g. via a bare `try/catch (Throwable)` or
        // `runCatching` without re-throwing CancellationException) the parent
        // coroutine would never see the cancel signal — structured concurrency
        // stalls.
        val cancel = CancellationException("test cancel")
        val persister = ThrowingPersister(cancel)

        assertFailsWith<CancellationException> {
            persister.loadWithOutcomes(automateContext, listOf("a").asFlow()).toList()
        }.also { thrown ->
            assertSame(cancel, thrown,
                "the exact CancellationException must propagate — no wrapping, no swallow")
        }
    }
}
