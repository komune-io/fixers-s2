package s2.sourcing.dsl.view

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import s2.dsl.automate.Evt
import s2.dsl.automate.model.WithS2Id
import s2.sourcing.dsl.event.EventRepository

class ViewLoaderTest {

    @Test
    fun testReloadHistoryWithComparableElement() = runTest {
        // Given
        val eventRepository = TestEventRepository()
        val view = TestView()
        val viewLoader = ViewLoader(eventRepository, view)

        // When
        val result = viewLoader.reloadHistory()

        // Then
        assertThat(result).hasSize(2)
        assertThat(result[0]).isEqualTo("Entity-1")
        assertThat(result[1]).isEqualTo("Entity-2")
    }

    @Test
    fun testReloadHistoryWithNonComparableElement() = runTest {
        // Given
        val eventRepository = TestEventNonComparableRepository()
        val view = TestNonComparableView()
        val viewLoader = ViewLoader(eventRepository, view)

        // When
        val result = viewLoader.reloadHistory()

        // Then
        assertThat(result).hasSize(2)
        // Verify that the order is preserved as loaded (Entity-2 first, then Entity-1)
        assertThat(result[0]).isEqualTo("Entity-2")
        assertThat(result[1]).isEqualTo("Entity-1")
    }

    class TestEvent(
        private val id: String,
        private val timestamp: Long
    ) : Evt, WithS2Id<String>, Comparable<TestEvent> {
        override fun s2Id(): String = id
        override fun compareTo(other: TestEvent): Int = timestamp.compareTo(other.timestamp)
    }

    class TestEventRepository : EventRepository<TestEvent, String> {
        override suspend fun load(id: String): Flow<TestEvent> {
            return when (id) {
                "1" -> flowOf(
                    TestEvent("1", 1),
                    TestEvent("1", 3)
                )
                "2" -> flowOf(
                    TestEvent("2", 2),
                    TestEvent("2", 4)
                )
                else -> flowOf()
            }
        }

        override suspend fun loadAll(): Flow<TestEvent> = flowOf(
            TestEvent("1", 3), // Out of order intentionally
            TestEvent("2", 2),
            TestEvent("1", 1), // Out of order intentionally
            TestEvent("2", 4)
        )

        override suspend fun persist(event: TestEvent): TestEvent = event
        override suspend fun persist(events: Flow<TestEvent>): Flow<TestEvent> = events
        override suspend fun createTable() {}
    }

    class TestView : View<TestEvent, String> {
        override suspend fun evolve(event: TestEvent, model: String?): String {
            return model ?: "Entity-${event.s2Id()}"
        }
    }

    // Non-comparable event class
    class TestEventNonComparable(
        private val id: String
    ) : Evt, WithS2Id<String> {
        override fun s2Id(): String = id
    }

    class TestEventNonComparableRepository : EventRepository<TestEventNonComparable, String> {
        override suspend fun load(id: String): Flow<TestEventNonComparable> {
            return when (id) {
                "1" -> flowOf(
                    TestEventNonComparable("1")
                )
                "2" -> flowOf(
                    TestEventNonComparable("2")
                )
                else -> flowOf()
            }
        }

        // Load events in a specific order: first Entity-2, then Entity-1
        // Since they don't implement Comparable, this order should be preserved
        override suspend fun loadAll(): Flow<TestEventNonComparable> = flowOf(
            TestEventNonComparable("2"),
            TestEventNonComparable("1")
        )

        override suspend fun persist(event: TestEventNonComparable): TestEventNonComparable = event
        override suspend fun persist(events: Flow<TestEventNonComparable>): Flow<TestEventNonComparable> = events
        override suspend fun createTable() {}
    }

    class TestNonComparableView : View<TestEventNonComparable, String> {
        override suspend fun evolve(event: TestEventNonComparable, model: String?): String {
            return model ?: "Entity-${event.s2Id()}"
        }
    }
}
