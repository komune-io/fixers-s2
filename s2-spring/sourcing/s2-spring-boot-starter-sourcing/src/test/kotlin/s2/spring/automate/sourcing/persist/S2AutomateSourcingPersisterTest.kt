package s2.spring.automate.sourcing.persist

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import s2.automate.core.config.S2BatchProperties
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.storing.snap.SnapPersister
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.builder.s2
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.sourcing.dsl.Loader
import s2.sourcing.dsl.event.EventRepository

/**
 * Unit tests for [S2AutomateSourcingPersister] exercising the (de-suspended)
 * cold-flow load/loadAll/persist paths with in-memory fakes. No Spring / DB.
 */
class S2AutomateSourcingPersisterTest {

    // ---- domain fixtures ----

    enum class TestState(override var position: Int) : S2State {
        Created(0)
    }

    data class TestEntity(val id: String) : WithS2Id<String>, WithS2State<TestState> {
        override fun s2Id(): String = id
        override fun s2State(): TestState = TestState.Created
    }

    data class TestEvent(val id: String) : Evt, WithS2Id<String> {
        override fun s2Id(): String = id
    }

    data class StubInitCmd(val id: String) : S2InitCommand
    data class StubCmd(override val id: String) : S2Command<String>

    private val automate: S2Automate = s2 { name = "SourcingPersisterTest" }
    private val automateContext = AutomateContext(automate, S2BatchProperties())

    // ---- fakes ----

    /** Echoes every persisted event back; records what was persisted. */
    private class FakeEventRepository : EventRepository<TestEvent, String> {
        val persisted = mutableListOf<TestEvent>()

        override fun load(id: String): Flow<TestEvent> = flowOf()
        override fun loadAll(): Flow<TestEvent> = flowOf()

        override suspend fun persist(event: TestEvent): TestEvent {
            persisted.add(event)
            return event
        }

        override fun persist(events: Flow<TestEvent>): Flow<TestEvent> = events.map { event ->
            persisted.add(event)
            event
        }

        override suspend fun createTable() = Unit
    }

    /** Rebuilds an entity from the event id. */
    private class FakeLoader : Loader<TestEvent, TestEntity, String> {
        override suspend fun load(id: String): TestEntity? = TestEntity(id)
        override suspend fun load(events: Flow<TestEvent>): TestEntity? =
            events.toList().lastOrNull()?.let { TestEntity(it.id) }
        override suspend fun loadAndEvolve(id: String, news: Flow<TestEvent>): TestEntity? =
            TestEntity(id)
        override suspend fun evolve(events: Flow<TestEvent>, entity: TestEntity?): TestEntity? =
            events.toList().lastOrNull()?.let { TestEntity(it.id) }
        override suspend fun reloadHistory(): List<TestEntity> = emptyList()
    }

    private fun persister(
        loader: Loader<TestEvent, TestEntity, String> = FakeLoader(),
        eventStore: FakeEventRepository = FakeEventRepository(),
    ): S2AutomateSourcingPersister<TestState, String, TestEntity, TestEvent> {
        val snapPersister = SnapPersister<TestState, String, TestEntity, TestEvent>(
            projectionLoader = loader,
            snapRepository = null,
            retryTaskChannel = null,
        )
        return S2AutomateSourcingPersister(
            projectionLoader = loader,
            eventStore = eventStore,
            snapPersister = snapPersister,
        )
    }

    private fun initCtx(id: String) =
        InitTransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>(
            automateContext = automateContext,
            msgId = id,
            msg = StubInitCmd(id),
            event = TestEvent(id),
            entity = TestEntity(id),
        )

    private fun transitionCtx(id: String) =
        TransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>(
            automateContext = automateContext,
            msgId = id,
            from = TestState.Created,
            msg = StubCmd(id),
            event = TestEvent(id),
            entity = TestEntity(id),
        )

    // ---- tests ----

    @Test
    suspend fun `load(id) delegates to the projection loader`() {
        val entity = persister().load(automateContext, "obj-1")
        assertThat(entity).isEqualTo(TestEntity("obj-1"))
    }

    @Test
    suspend fun `load(ids) maps each id through the projection loader`() {
        val loaded = persister()
            .load(automateContext, flowOf("a", "b", "c"))
            .toList()
        assertThat(loaded).containsExactly(TestEntity("a"), TestEntity("b"), TestEntity("c"))
    }

    @Test
    suspend fun `persistInit persists events through the event store and returns them`() {
        val store = FakeEventRepository()
        val events = persister(eventStore = store)
            .persistInit(flowOf(initCtx("e1"), initCtx("e2")))
            .toList()

        assertThat(events).containsExactly(TestEvent("e1"), TestEvent("e2"))
        assertThat(store.persisted).containsExactly(TestEvent("e1"), TestEvent("e2"))
    }

    @Test
    suspend fun `persist persists transition events through the event store and returns them`() {
        val store = FakeEventRepository()
        val events = persister(eventStore = store)
            .persist(flowOf(transitionCtx("t1"), transitionCtx("t2")))
            .toList()

        assertThat(events).containsExactly(TestEvent("t1"), TestEvent("t2"))
        assertThat(store.persisted).containsExactly(TestEvent("t1"), TestEvent("t2"))
    }
}
