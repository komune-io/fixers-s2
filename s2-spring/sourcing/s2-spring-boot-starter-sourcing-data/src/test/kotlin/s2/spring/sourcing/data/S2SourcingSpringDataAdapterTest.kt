package s2.spring.sourcing.data

import java.util.Collections
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.sourcing.dsl.event.EventRepository
import s2.spring.automate.sourcing.S2AutomateDeciderSpring
import s2.spring.sourcing.data.event.EventRepositoryFactory

class S2SourcingSpringDataAdapterTest {

	enum class TestState(override val position: Int) : S2State { Created(0) }

	data class TestEntity(val id: String) : WithS2Id<String>, WithS2State<TestState> {
		override fun s2Id() = id
		override fun s2State() = TestState.Created
	}

	data class TestEvent(val id: String) : Evt, WithS2Id<String> {
		override fun s2Id() = id
	}

	open class TestAdapter : S2SourcingSpringDataAdapter<TestEntity, TestState, TestEvent, String,
			S2AutomateDeciderSpring<TestEntity, TestState, TestEvent, String>>(
		S2AutomateDeciderSpring(), { _, model -> model }
	) {
		override fun automate(): S2Automate = error("not used")
		override fun entityType(): KClass<TestEvent> = TestEvent::class
	}

	class RecordingRepository(
		private val tableDispatcher: CoroutineDispatcher? = null,
		private val failingCreates: Int = 0,
	) : EventRepository<TestEvent, String> {
		val calls: MutableList<String> = Collections.synchronizedList(mutableListOf())
		private val creates = AtomicInteger()

		override suspend fun createTable() {
			if (tableDispatcher != null) withContext(tableDispatcher) { delay(1) } else delay(1)
			if (creates.incrementAndGet() <= failingCreates) error("table not created")
			calls += "createTable"
		}

		override suspend fun load(id: String): Flow<TestEvent> = emptyFlow<TestEvent>().also { calls += "load" }
		override suspend fun loadAll(): Flow<TestEvent> = emptyFlow<TestEvent>().also { calls += "loadAll" }
		override suspend fun persist(event: TestEvent): TestEvent = event.also { calls += "persist" }
		override suspend fun persist(events: Flow<TestEvent>): Flow<TestEvent> = events.also { calls += "persistAll" }
	}

	class CountingFactory(private val repository: RecordingRepository) : EventRepositoryFactory {
		val created = AtomicInteger()

		@Suppress("UNCHECKED_CAST")
		override fun <EVENT, ID> create(eventType: KClass<EVENT>, json: Json): EventRepository<EVENT, ID>
				where EVENT : Evt, EVENT : WithS2Id<ID> {
			created.incrementAndGet()
			return repository as EventRepository<EVENT, ID>
		}
	}

	private val loop = Executors.newSingleThreadExecutor { Thread(it, "event-loop").apply { isDaemon = true } }

	@AfterEach
	fun stopLoop() {
		loop.shutdownNow()
	}

	private fun adapter(repository: RecordingRepository) =
		TestAdapter().apply { eventRepositoryFactory = CountingFactory(repository) }

	@Test
	fun `eventStore builds the repository once`() {
		val adapter = adapter(RecordingRepository())

		val first = adapter.eventStore()
		val second = adapter.eventStore()

		assertThat(second).isSameAs(first)
		assertThat((adapter.eventRepositoryFactory as CountingFactory).created.get()).isEqualTo(1)
	}

	@Test
	fun `eventStore does not create the table on the calling thread`() {
		val repository = RecordingRepository()

		adapter(repository).eventStore()

		assertThat(repository.calls).isEmpty()
	}

	@Test
	fun `eventStore called from a single-threaded event loop does not deadlock`() {
		val dispatcher = loop.asCoroutineDispatcher()
		val adapter = adapter(RecordingRepository(tableDispatcher = dispatcher))

		val store = CompletableFuture.supplyAsync({ adapter.eventStore() }, loop)

		assertThat(store.get(5, TimeUnit.SECONDS)).isNotNull
	}

	@Test
	suspend fun `startup creates the table once, before any operation`() {
		val repository = RecordingRepository()
		val adapter = adapter(repository)
		adapter.eventStore()

		adapter.afterSingletonsInstantiated()
		assertThat(repository.calls).containsExactly("createTable")
		adapter.eventStore().persist(TestEvent("1"))

		assertThat(repository.calls).containsExactly("createTable", "persist")
	}

	@Test
	fun `a subclass caching super eventStore shares the one store`() {
		val repository = RecordingRepository()
		val adapter = object : TestAdapter() {
			private val store by lazy { super.eventStore() }
			override fun eventStore() = store
		}.apply { eventRepositoryFactory = CountingFactory(repository) }

		val first = adapter.eventStore()
		adapter.afterSingletonsInstantiated()

		assertThat(adapter.eventStore()).isSameAs(first)
		assertThat((adapter.eventRepositoryFactory as CountingFactory).created.get()).isEqualTo(1)
		assertThat(repository.calls).containsExactly("createTable")
	}

	@Test
	fun `a subclass with its own store gets no table from startup`() {
		val repository = RecordingRepository()
		val own = RecordingRepository()
		val adapter = object : TestAdapter() {
			override fun eventStore() = own
		}.apply { eventRepositoryFactory = CountingFactory(repository) }

		adapter.eventStore()
		adapter.afterSingletonsInstantiated()

		assertThat((adapter.eventRepositoryFactory as CountingFactory).created.get()).isZero()
		assertThat(own.calls).isEmpty()
	}

	@Test
	suspend fun `the table exists before the first persist and is created once`() {
		val repository = RecordingRepository()
		val store = adapter(repository).eventStore()

		store.persist(TestEvent("1"))
		store.persist(TestEvent("2"))
		store.load("1").toList()

		assertThat(repository.calls).containsExactly("createTable", "persist", "persist", "load")
	}

	@Test
	suspend fun `every operation creates the table first`() {
		suspend fun firstCall(operation: suspend (EventRepository<TestEvent, String>) -> Unit): List<String> {
			val repository = RecordingRepository()
			operation(adapter(repository).eventStore())
			return repository.calls.toList()
		}

		assertThat(firstCall { it.load("1") }).containsExactly("createTable", "load")
		assertThat(firstCall { it.loadAll() }).containsExactly("createTable", "loadAll")
		assertThat(firstCall { it.persist(flowOf(TestEvent("1"))) }).containsExactly("createTable", "persistAll")
		assertThat(firstCall { it.createTable() }).containsExactly("createTable")
	}

	@Test
	suspend fun `concurrent first operations create the table once`() {
		val repository = RecordingRepository()
		val store = adapter(repository).eventStore()

		withContext(Dispatchers.Default) {
			coroutineScope {
				(1..20).map { async { store.persist(TestEvent("$it")) } }.awaitAll()
			}
		}

		assertThat(repository.calls.count { it == "createTable" }).isEqualTo(1)
		assertThat(repository.calls.first()).isEqualTo("createTable")
	}

	@Test
	suspend fun `a failed table creation is retried by the next operation`() {
		val repository = RecordingRepository(failingCreates = 1)
		val store = adapter(repository).eventStore()

		val failure = runCatching { store.persist(TestEvent("1")) }.exceptionOrNull()
		assertThat(failure).hasMessage("table not created")
		store.persist(TestEvent("2"))

		assertThat(repository.calls).containsExactly("createTable", "persist")
	}
}
