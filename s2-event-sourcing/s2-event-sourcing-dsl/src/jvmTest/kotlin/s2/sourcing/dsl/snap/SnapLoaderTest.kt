package s2.sourcing.dsl.snap

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import s2.dsl.automate.Evt
import s2.dsl.automate.model.WithS2Id
import s2.sourcing.dsl.event.EventRepository
import s2.sourcing.dsl.view.View
import s2.sourcing.dsl.view.ViewLoader

class SnapLoaderTest {

    data class TestEvent(val id: String, val value: Int) : Evt, WithS2Id<String> {
        override fun s2Id(): String = id
    }

    data class TestEntity(val id: String, val total: Int) : WithS2Id<String> {
        override fun s2Id(): String = id
    }

    private class StubEventRepository(
        private val events: List<TestEvent>,
    ) : EventRepository<TestEvent, String> {
        override suspend fun load(id: String): Flow<TestEvent> =
            flowOf(*events.filter { it.s2Id() == id }.toTypedArray())

        override suspend fun loadAll(): Flow<TestEvent> = flowOf(*events.toTypedArray())
        override suspend fun persist(event: TestEvent): TestEvent = event
        override suspend fun persist(events: Flow<TestEvent>): Flow<TestEvent> = events
        override suspend fun createTable() { /* no-op for the stub */ }
    }

    private val view = View<TestEvent, TestEntity> { event, model ->
        TestEntity(event.s2Id(), (model?.total ?: 0) + event.value)
    }

    private class InMemorySnapRepository : SnapRepository<TestEntity, String> {
        val store = mutableMapOf<String, TestEntity>()
        val removed = mutableListOf<String>()
        override suspend fun get(id: String): TestEntity? = store[id]
        override suspend fun save(entity: TestEntity): TestEntity {
            store[entity.s2Id()] = entity
            return entity
        }

        override suspend fun remove(id: String): Boolean {
            removed.add(id)
            return store.remove(id) != null
        }
    }

    private fun snapLoader(
        snapRepository: InMemorySnapRepository = InMemorySnapRepository(),
        events: List<TestEvent> = listOf(TestEvent("1", 10), TestEvent("2", 20)),
    ): SnapLoader<TestEvent, TestEntity, String> {
        val viewLoader = ViewLoader(StubEventRepository(events), view)
        return SnapLoader(snapRepository, viewLoader)
    }

    @Test
    suspend fun `load by id prefers the snapshot`() {
        val snapRepository = InMemorySnapRepository()
        snapRepository.save(TestEntity("1", 999))
        val loader = snapLoader(snapRepository)
        assertThat(loader.load("1")).isEqualTo(TestEntity("1", 999))
    }

    @Test
    suspend fun `load by id falls back to the view loader on snapshot miss`() {
        val loader = snapLoader()
        assertThat(loader.load("1")).isEqualTo(TestEntity("1", 10))
    }

    @Test
    suspend fun `loadAndEvolve applies news on top of the loaded entity`() {
        val loader = snapLoader()
        val entity = loader.loadAndEvolve("1", flowOf(TestEvent("1", 5)))
        assertThat(entity).isEqualTo(TestEntity("1", 15))
    }

    @Test
    suspend fun `load from events replays through the view`() {
        val loader = snapLoader()
        val entity = loader.load(flowOf(TestEvent("9", 1), TestEvent("9", 2)))
        assertThat(entity).isEqualTo(TestEntity("9", 3))
    }

    @Test
    suspend fun `evolve delegates to the view loader`() {
        val loader = snapLoader()
        val entity = loader.evolve(flowOf(TestEvent("1", 1)), TestEntity("1", 41))
        assertThat(entity).isEqualTo(TestEntity("1", 42))
    }

    @Test
    suspend fun `reloadHistory rebuilds entities and refreshes snapshots`() {
        val snapRepository = InMemorySnapRepository()
        val loader = snapLoader(snapRepository)
        val entities = loader.reloadHistory()
        assertThat(entities).containsExactlyInAnyOrder(TestEntity("1", 10), TestEntity("2", 20))
        assertThat(snapRepository.store).hasSize(2)
        assertThat(snapRepository.removed).containsExactlyInAnyOrder("1", "2")
    }
}
