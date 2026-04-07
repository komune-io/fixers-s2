package s2.spring.sourcing.data.r2dbc

import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.r2dbc.core.DatabaseClient
import s2.dsl.automate.Evt
import s2.dsl.automate.model.WithS2Id
import s2.spring.sourcing.data.r2dbc.config.SpringTestBase

@Serializable
data class TestEvent(
    val id: String,
    val name: String,
    val timestamp: Long = System.currentTimeMillis()
) : Evt, WithS2Id<String> {
    override fun s2Id() = id
}

class R2dbcEventRepositoryTest : SpringTestBase() {

    @Autowired
    private lateinit var databaseClient: DatabaseClient

    @Autowired
    private lateinit var r2dbcEntityTemplate: R2dbcEntityTemplate

    private lateinit var eventRepository: R2dbcEventRepository<TestEvent, String>

    private val json = Json { ignoreUnknownKeys = true }

    @BeforeEach
    fun setup() = runBlocking {
        eventRepository = R2dbcEventRepository(
            json = json,
            databaseClient = databaseClient,
            r2dbcEntityTemplate = r2dbcEntityTemplate,
            eventType = TestEvent::class
        )

        // Create the table
        eventRepository.createTable()
    }

    @Test
    fun `should load events in ascending order by created_date`(): Unit = runBlocking {
        // Given
        val objId = UUID.randomUUID().toString()

        // Create events with different timestamps
        val event3 = TestEvent(id = objId, name = "Event 3", timestamp = 300)
        val event1 = TestEvent(id = objId, name = "Event 1", timestamp = 100)
        val event2 = TestEvent(id = objId, name = "Event 2", timestamp = 200)

        // When - persist events in random order with delays to ensure different created_date values
        eventRepository.persist(event1)
        delay(100) // Add delay to ensure different created_date values
        eventRepository.persist(event2)
        delay(100) // Add delay to ensure different created_date values
        eventRepository.persist(event3)

        // Then - events should be loaded in ascending order by created_date
        val loadedEvents = eventRepository.load(objId).toList()

        // Assert that events are ordered by created_date (which should be set automatically)
        assertThat(loadedEvents).hasSize(3)

        // Check the order of events by name (which should correspond to the order of created_date)
        assertThat(loadedEvents[0].name).isEqualTo("Event 1")
        assertThat(loadedEvents[1].name).isEqualTo("Event 2")
        assertThat(loadedEvents[2].name).isEqualTo("Event 3")
    }

    @Test
    fun `should load all events in ascending order by created_date`(): Unit = runBlocking {
        // Given
        val objId1 = UUID.randomUUID().toString()
        val objId2 = UUID.randomUUID().toString()

        // Create events with different timestamps for different objects
        val event1 = TestEvent(id = objId1, name = "Event 1", timestamp = 100)
        val event2 = TestEvent(id = objId1, name = "Event 2", timestamp = 200)
        val event3 = TestEvent(id = objId2, name = "Event 3", timestamp = 150)

        // When - persist events in random order with delays to ensure different created_date values
        eventRepository.persist(event1)
        delay(100) // Add delay to ensure different created_date values
        eventRepository.persist(event3)
        delay(100) // Add delay to ensure different created_date values
        eventRepository.persist(event2)

        // Then - all events should be loaded in ascending order by created_date
        val loadedEvents = eventRepository.loadAll().toList()

        // Assert that all events are loaded
        assertThat(loadedEvents).hasSize(3)

        // Check the order of events by timestamp (which should correspond to the order of created_date)
        // Note: This assumes that created_date is set close to the time of persistence
        // and that the events are persisted with enough time difference to ensure ordering
        assertThat(loadedEvents[0].name).isEqualTo("Event 1")
        assertThat(loadedEvents[1].name).isEqualTo("Event 3")
        assertThat(loadedEvents[2].name).isEqualTo("Event 2")
    }
}
