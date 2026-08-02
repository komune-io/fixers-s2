package s2.automate.core.storing.snap

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.fold
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import s2.automate.core.error.AutomateException
import s2.dsl.automate.Evt
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.sourcing.dsl.Loader
import s2.sourcing.dsl.snap.SnapRepository

class SnapPersisterTest {

    enum class TestState(override val position: Int) : S2State {
        Created(0)
    }

    data class TestEntity(val id: String, val state: TestState = TestState.Created) :
        WithS2Id<String>, WithS2State<TestState> {
        override fun s2Id(): String = id
        override fun s2State(): TestState = state
    }

    data class TestEvent(val id: String) : Evt, WithS2Id<String> {
        override fun s2Id(): String = id
    }

    private class StubLoader(
        private val found: Boolean = true,
    ) : Loader<TestEvent, TestEntity, String> {
        override suspend fun load(id: String): TestEntity? = if (found) TestEntity(id) else null
        override suspend fun load(events: Flow<TestEvent>): TestEntity? = evolve(events)
        override suspend fun loadAndEvolve(id: String, news: Flow<TestEvent>): TestEntity? =
            if (found) news.fold(TestEntity(id)) { entity, _ -> entity } else null

        override suspend fun evolve(events: Flow<TestEvent>, entity: TestEntity?): TestEntity? =
            events.fold(entity) { _, event -> TestEntity(event.s2Id()) }

        override suspend fun reloadHistory(): List<TestEntity> = emptyList()
    }

    private class RecordingSnapRepository : SnapRepository<TestEntity, String> {
        val saved = mutableListOf<TestEntity>()
        override suspend fun get(id: String): TestEntity? = null
        override suspend fun save(entity: TestEntity): TestEntity {
            saved.add(entity)
            return entity.copy(id = "saved-${entity.id}")
        }

        override suspend fun remove(id: String): Boolean = false
    }

    @Test
    suspend fun `persist evolves the projection and saves the snapshot`() {
        val snapRepository = RecordingSnapRepository()
        val persister = SnapPersister<TestState, String, TestEntity, TestEvent>(
            projectionLoader = StubLoader(),
            snapRepository = snapRepository,
            retryTaskChannel = null,
        )
        val (entity, event) = persister.persist(TestEvent("1"))
        assertEquals("saved-1", entity.id)
        assertEquals("1", event.id)
        assertEquals(1, snapRepository.saved.size)
    }

    @Test
    suspend fun `persist without snap repository returns the mutated entity`() {
        val persister = SnapPersister<TestState, String, TestEntity, TestEvent>(
            projectionLoader = StubLoader(),
            snapRepository = null,
            retryTaskChannel = null,
        )
        val (entity, _) = persister.persist(TestEvent("2"))
        assertEquals("2", entity.id)
    }

    @Test
    suspend fun `persist routes through the retry task channel when configured`() {
        val channel = RetryTaskChannel(maxAttempts = 2, delayMillis = 1, retryOn = IllegalStateException::class)
        val persister = SnapPersister<TestState, String, TestEntity, TestEvent>(
            projectionLoader = StubLoader(),
            snapRepository = null,
            retryTaskChannel = channel,
        )
        val (entity, event) = persister.persist(TestEvent("3"))
        assertEquals("3", entity.id)
        assertEquals("3", event.id)
        channel.cancelAllCoroutines()
    }

    @Test
    suspend fun `persist throws when the projection cannot be loaded`() {
        val persister = SnapPersister<TestState, String, TestEntity, TestEvent>(
            projectionLoader = StubLoader(found = false),
            snapRepository = null,
            retryTaskChannel = null,
        )
        val exception = assertThrows<AutomateException> {
            persister.persist(TestEvent("4"))
        }
        assertTrue(exception.errors.single().type == "ERROR_ENTITY_NOT_FOUND")
    }
}
