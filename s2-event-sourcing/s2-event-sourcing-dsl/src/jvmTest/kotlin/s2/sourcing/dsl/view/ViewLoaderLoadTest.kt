package s2.sourcing.dsl.view

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import s2.dsl.automate.Evt
import s2.dsl.automate.model.WithS2Id
import s2.sourcing.dsl.event.EventRepository

class ViewLoaderLoadTest {

    data class TestEvent(val id: String, val value: Int) : Evt, WithS2Id<String> {
        override fun s2Id(): String = id
    }

    data class TestEntity(val id: String, val total: Int)

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

    private fun loader(events: List<TestEvent> = listOf(TestEvent("1", 1), TestEvent("1", 2))) =
        ViewLoader(StubEventRepository(events), view)

    @Test
    suspend fun `load by id folds all events of the entity`() {
        assertThat(loader().load("1")).isEqualTo(TestEntity("1", 3))
    }

    @Test
    suspend fun `load by id returns null when there is no event`() {
        assertThat(loader().load("missing")).isNull()
    }

    @Test
    suspend fun `loadAndEvolve folds history then the news`() {
        val entity = loader().loadAndEvolve("1", flowOf(TestEvent("1", 4)))
        assertThat(entity).isEqualTo(TestEntity("1", 7))
    }

    @Test
    suspend fun `evolve starts from the provided entity`() {
        val entity = loader().evolve(flowOf(TestEvent("1", 1)), TestEntity("1", 10))
        assertThat(entity).isEqualTo(TestEntity("1", 11))
    }
}
