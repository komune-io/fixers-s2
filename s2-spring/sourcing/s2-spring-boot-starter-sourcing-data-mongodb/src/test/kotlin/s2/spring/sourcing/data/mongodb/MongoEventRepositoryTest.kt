package s2.spring.sourcing.data.mongodb

import java.util.UUID
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import s2.dsl.automate.Evt
import s2.dsl.automate.model.WithS2Id
import s2.sourcing.dsl.event.EventRepository
import s2.spring.sourcing.data.mongodb.config.SpringTestBase

@Serializable
data class TestEvent(
    val id: String,
    val name: String,
    val timestamp: Long = System.currentTimeMillis()
) : Evt, WithS2Id<String> {
    override fun s2Id() = id
}

class MongoEventRepositoryTest : SpringTestBase() {

    @Autowired
    private lateinit var mongoOperations: ReactiveMongoOperations

    private lateinit var eventRepository: EventRepository<TestEvent, String>

    private val json = Json { ignoreUnknownKeys = true }

    @BeforeEach
    suspend fun setup() {
        eventRepository = MongoEventRepositoryFactory(mongoOperations)
            .create(TestEvent::class, json)
        eventRepository.createTable() // no-op for Mongo
        // Clean up any previously persisted documents.
        mongoOperations.dropCollection("eventSourcing").block()
    }

    @Test
    suspend fun `persist single event and load it back by object id (encode-decode round-trip)`() {
        val objId = UUID.randomUUID().toString()
        val event = TestEvent(id = objId, name = "Created")

        val persisted = eventRepository.persist(event)
        assertThat(persisted).isEqualTo(event)

        val loaded = eventRepository.load(objId).toList()
        assertThat(loaded).containsExactly(event)
    }

    @Test
    suspend fun `persist a flow of events and load all back`() {
        val objId1 = UUID.randomUUID().toString()
        val objId2 = UUID.randomUUID().toString()

        val event1 = TestEvent(id = objId1, name = "Event 1", timestamp = 100)
        val event2 = TestEvent(id = objId1, name = "Event 2", timestamp = 200)
        val event3 = TestEvent(id = objId2, name = "Event 3", timestamp = 150)

        val returned = eventRepository.persist(flowOf(event1, event2, event3)).toList()
        assertThat(returned).containsExactlyInAnyOrder(event1, event2, event3)

        val allEvents = eventRepository.loadAll().toList()
        assertThat(allEvents).containsExactlyInAnyOrder(event1, event2, event3)

        val forObj1 = eventRepository.load(objId1).toList()
        assertThat(forObj1).containsExactlyInAnyOrder(event1, event2)
    }
}
