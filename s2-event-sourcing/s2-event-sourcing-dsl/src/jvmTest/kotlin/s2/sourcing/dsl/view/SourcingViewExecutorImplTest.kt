package s2.sourcing.dsl.view

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import s2.dsl.automate.Evt
import s2.dsl.automate.model.WithS2Id
import s2.sourcing.dsl.event.EventRepository
import s2.sourcing.dsl.snap.SnapRepository

class SourcingViewExecutorImplTest {

    data class TestEvent(val id: String, val value: Int) : Evt, WithS2Id<String> {
        override fun s2Id(): String = id
    }

    data class TestEntity(val id: String, val total: Int) : WithS2Id<String> {
        override fun s2Id(): String = id
    }

    private class StubEventRepository : EventRepository<TestEvent, String> {
        override fun load(id: String): Flow<TestEvent> = flowOf(TestEvent(id, 10))
        override fun loadAll(): Flow<TestEvent> = flowOf()
        override suspend fun persist(event: TestEvent): TestEvent = event
        override fun persist(events: Flow<TestEvent>): Flow<TestEvent> = events
        override suspend fun createTable() { /* no-op for the stub */ }
    }

    private class RecordingView : View<TestEvent, TestEntity> {
        val evolved = mutableListOf<Pair<TestEvent, TestEntity?>>()
        override suspend fun evolve(event: TestEvent, model: TestEntity?): TestEntity {
            evolved.add(event to model)
            return TestEntity(event.s2Id(), (model?.total ?: 0) + event.value)
        }
    }

    private class StubSnapRepository(
        private val entity: TestEntity? = null,
    ) : SnapRepository<TestEntity, String> {
        override suspend fun get(id: String): TestEntity? = entity
        override suspend fun save(entity: TestEntity): TestEntity = entity
        override suspend fun remove(id: String): Boolean = false
    }

    @Test
    suspend fun `evolve uses the snapshot when available`() {
        val view = RecordingView()
        val snapshot = TestEntity("1", 100)
        val executor = SourcingViewExecutorImpl(
            evolver = view,
            viewBuilder = ViewLoader(StubEventRepository(), view),
            viewRepository = StubSnapRepository(snapshot),
        )
        executor.evolve("1", TestEvent("1", 1))
        assertThat(view.evolved.last().second).isEqualTo(snapshot)
    }

    @Test
    suspend fun `evolve rebuilds the entity from events on snapshot miss`() {
        val view = RecordingView()
        val executor = SourcingViewExecutorImpl(
            evolver = view,
            viewBuilder = ViewLoader(StubEventRepository(), view),
            viewRepository = StubSnapRepository(entity = null),
        )
        executor.evolve("1", TestEvent("1", 1))
        // First evolve call comes from the ViewLoader rebuild, the last from the executor itself.
        assertThat(view.evolved.last().second).isEqualTo(TestEntity("1", 10))
    }
}
