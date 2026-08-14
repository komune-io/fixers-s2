package s2.spring.automate.data.persister

import java.util.Optional
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.reactivestreams.Publisher
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import s2.automate.core.config.S2BatchProperties
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.TransitionAppliedContext
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

class SpringDataPersisterFlowTest {

    enum class TestState(override val position: Int) : S2State {
        Created(0)
    }

    data class TestEntity(val id: String, val state: TestState = TestState.Created) :
        WithS2Id<String>, WithS2State<TestState> {
        override fun s2Id(): String = id
        override fun s2State(): TestState = state
    }

    data class TestEvent(val id: String) : Evt

    data class CreateCmd(val id: String) : S2InitCommand
    data class DoCmd(override val id: String) : S2Command<String>

    private val automate = S2Automate(name = "PersisterTest", version = null, transitions = emptyArray())
    private val automateContext = AutomateContext(automate, S2BatchProperties(size = 2))

    private fun initContext(id: String) =
        InitTransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>(
            automateContext = automateContext,
            msgId = "msg-$id",
            msg = CreateCmd(id),
            event = TestEvent(id),
            entity = TestEntity(id),
        )

    private fun transitionContext(id: String) =
        TransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>(
            automateContext = automateContext,
            msgId = "msg-$id",
            from = TestState.Created,
            msg = DoCmd(id),
            event = TestEvent(id),
            entity = TestEntity(id),
        )

    // ---- coroutine repository ----

    private class InMemoryCoroutineRepository(
        initial: Map<String, TestEntity> = emptyMap(),
    ) : CoroutineCrudRepository<TestEntity, String> {
        val store = initial.toMutableMap()

        /** One entry per `saveAll(Iterable)` call, holding the ids saved by that call. */
        val saveAllBatches = mutableListOf<List<String>>()

        override suspend fun <S : TestEntity> save(entity: S): TestEntity {
            store[entity.s2Id()] = entity
            return entity
        }

        override fun <S : TestEntity> saveAll(entities: Iterable<S>): Flow<S> {
            saveAllBatches.add(entities.map { it.s2Id() })
            entities.forEach { store[it.s2Id()] = it }
            return entities.toList().asFlow()
        }

        override fun <S : TestEntity> saveAll(entityStream: Flow<S>): Flow<S> = entityStream
        override suspend fun findById(id: String): TestEntity? = store[id]
        override suspend fun existsById(id: String): Boolean = store.containsKey(id)
        override fun findAll(): Flow<TestEntity> = store.values.toList().asFlow()
        override fun findAllById(ids: Iterable<String>): Flow<TestEntity> =
            ids.mapNotNull { store[it] }.asFlow()

        override fun findAllById(ids: Flow<String>): Flow<TestEntity> =
            store.values.toList().asFlow()

        override suspend fun count(): Long = store.size.toLong()
        override suspend fun deleteById(id: String) = Unit
        override suspend fun delete(entity: TestEntity) = Unit
        override suspend fun deleteAllById(ids: Iterable<String>) = Unit
        override suspend fun deleteAll(entities: Iterable<TestEntity>) = Unit
        override suspend fun <S : TestEntity> deleteAll(entityStream: Flow<S>) = Unit
        override suspend fun deleteAll() = Unit
    }

    @Test
    suspend fun `coroutine persister saves entities and emits events on persistInit`() {
        val repository = InMemoryCoroutineRepository()
        val persister = SpringDataAutomateCoroutinePersisterFlow<TestState, String, TestEntity, TestEvent>(
            repository, S2BatchProperties(size = 2),
        )
        val events = persister.persistInit(flowOf(initContext("1"), initContext("2"), initContext("3"))).toList()
        assertThat(events.map { it.id }).containsExactly("1", "2", "3")
        assertThat(repository.store.keys).containsExactlyInAnyOrder("1", "2", "3")
    }

    @Test
    suspend fun `coroutine persister saves entities and emits events on persist`() {
        val repository = InMemoryCoroutineRepository()
        val persister = SpringDataAutomateCoroutinePersisterFlow<TestState, String, TestEntity, TestEvent>(
            repository, S2BatchProperties(size = 2),
        )
        val events = persister.persist(flowOf(transitionContext("1"), transitionContext("2"))).toList()
        assertThat(events.map { it.id }).containsExactly("1", "2")
        assertThat(repository.store.keys).containsExactlyInAnyOrder("1", "2")
    }

    @Test
    suspend fun `coroutine persister persists in chunks instead of buffering the whole flow`() {
        val repository = InMemoryCoroutineRepository()
        val persister = SpringDataAutomateCoroutinePersisterFlow<TestState, String, TestEntity, TestEvent>(
            repository, S2BatchProperties(size = 2, concurrency = 1),
        )
        val contexts = (1..5).map { transitionContext("$it") }
        val events = persister.persist(contexts.asFlow()).toList()

        // one saveAll per chunk of `size`, not a single saveAll holding the whole flow
        assertThat(repository.saveAllBatches)
            .containsExactly(listOf("1", "2"), listOf("3", "4"), listOf("5"))
        assertThat(events.map { it.id }).containsExactly("1", "2", "3", "4", "5")
    }

    @Test
    suspend fun `coroutine persister persistInit persists in chunks`() {
        val repository = InMemoryCoroutineRepository()
        val persister = SpringDataAutomateCoroutinePersisterFlow<TestState, String, TestEntity, TestEvent>(
            repository, S2BatchProperties(size = 2, concurrency = 1),
        )
        val events = persister.persistInit((1..5).map { initContext("$it") }.asFlow()).toList()

        assertThat(repository.saveAllBatches)
            .containsExactly(listOf("1", "2"), listOf("3", "4"), listOf("5"))
        assertThat(events.map { it.id }).containsExactly("1", "2", "3", "4", "5")
    }

    @Test
    suspend fun `coroutine persister loads by id and by ids`() {
        val repository = InMemoryCoroutineRepository(mapOf("1" to TestEntity("1")))
        val persister = SpringDataAutomateCoroutinePersisterFlow<TestState, String, TestEntity, TestEvent>(
            repository, S2BatchProperties(),
        )
        assertThat(persister.load(automateContext, "1")).isEqualTo(TestEntity("1"))
        assertThat(persister.load(automateContext, flowOf("1")).toList()).containsExactly(TestEntity("1"))
    }

    // ---- blocking repository ----

    private class InMemoryBlockingRepository(
        initial: Map<String, TestEntity> = emptyMap(),
    ) : CrudRepository<TestEntity, String> {
        val store = initial.toMutableMap()
        override fun <S : TestEntity> save(entity: S): S {
            store[entity.s2Id()] = entity
            return entity
        }

        override fun <S : TestEntity> saveAll(entities: Iterable<S>): Iterable<S> {
            entities.forEach { store[it.s2Id()] = it }
            return entities
        }

        override fun findById(id: String): Optional<TestEntity> = Optional.ofNullable(store[id])
        override fun existsById(id: String): Boolean = store.containsKey(id)
        override fun findAll(): Iterable<TestEntity> = store.values
        override fun findAllById(ids: Iterable<String>): Iterable<TestEntity> = ids.mapNotNull { store[it] }
        override fun count(): Long = store.size.toLong()
        override fun deleteById(id: String) = Unit
        override fun delete(entity: TestEntity) = Unit
        override fun deleteAllById(ids: Iterable<String>) = Unit
        override fun deleteAll(entities: Iterable<TestEntity>) = Unit
        override fun deleteAll() = Unit
    }

    @Test
    suspend fun `blocking persister saves entities and emits events`() {
        val repository = InMemoryBlockingRepository()
        val persister = SpringDataAutomatePersisterFlow<TestState, String, TestEntity, TestEvent>(repository)
        val initEvents = persister.persistInit(flowOf(initContext("1"))).toList()
        assertThat(initEvents.map { it.id }).containsExactly("1")
        val events = persister.persist(flowOf(transitionContext("2"))).toList()
        assertThat(events.map { it.id }).containsExactly("2")
        assertThat(repository.store.keys).containsExactlyInAnyOrder("1", "2")
    }

    @Test
    suspend fun `blocking persister loads by id and by ids`() {
        val repository = InMemoryBlockingRepository(mapOf("1" to TestEntity("1")))
        val persister = SpringDataAutomatePersisterFlow<TestState, String, TestEntity, TestEvent>(repository)
        assertThat(persister.load(automateContext, "1")).isEqualTo(TestEntity("1"))
        assertThat(persister.load(automateContext, flowOf("1")).toList()).containsExactly(TestEntity("1"))
    }

    // ---- reactive repository ----

    private class InMemoryReactiveRepository(
        initial: Map<String, TestEntity> = emptyMap(),
    ) : ReactiveCrudRepository<TestEntity, String> {
        val store = initial.toMutableMap()
        override fun <S : TestEntity> save(entity: S): Mono<S> {
            store[entity.s2Id()] = entity
            return Mono.just(entity)
        }

        override fun <S : TestEntity> saveAll(entities: Iterable<S>): Flux<S> {
            entities.forEach { store[it.s2Id()] = it }
            return Flux.fromIterable(entities)
        }

        override fun <S : TestEntity> saveAll(entityStream: Publisher<S>): Flux<S> = Flux.from(entityStream)
        override fun findById(id: String): Mono<TestEntity> = Mono.justOrEmpty(store[id])
        override fun findById(id: Publisher<String>): Mono<TestEntity> = Mono.from(id).flatMap { findById(it) }
        override fun existsById(id: String): Mono<Boolean> = Mono.just(store.containsKey(id))
        override fun existsById(id: Publisher<String>): Mono<Boolean> = Mono.from(id).flatMap { existsById(it) }
        override fun findAll(): Flux<TestEntity> = Flux.fromIterable(store.values)
        override fun findAllById(ids: Iterable<String>): Flux<TestEntity> =
            Flux.fromIterable(ids.mapNotNull { store[it] })

        override fun findAllById(idStream: Publisher<String>): Flux<TestEntity> =
            Flux.from(idStream).mapNotNull { store[it] }

        override fun count(): Mono<Long> = Mono.just(store.size.toLong())
        override fun deleteById(id: String): Mono<Void> = Mono.empty()
        override fun deleteById(id: Publisher<String>): Mono<Void> = Mono.empty()
        override fun delete(entity: TestEntity): Mono<Void> = Mono.empty()
        override fun deleteAllById(ids: Iterable<String>): Mono<Void> = Mono.empty()
        override fun deleteAll(entities: Iterable<TestEntity>): Mono<Void> = Mono.empty()
        override fun deleteAll(entityStream: Publisher<out TestEntity>): Mono<Void> = Mono.empty()
        override fun deleteAll(): Mono<Void> = Mono.empty()
    }

    @Test
    suspend fun `reactive persister saves entities and emits events on persistInit`() {
        val repository = InMemoryReactiveRepository()
        val persister = SpringDataAutomateReactivePersisterFlow<TestState, String, TestEntity, TestEvent>(
            repository, S2BatchProperties(size = 2),
        )
        val events = persister.persistInit(flowOf(initContext("1"), initContext("2"), initContext("3"))).toList()
        assertThat(events.map { it.id }).containsExactly("1", "2", "3")
        assertThat(repository.store.keys).containsExactlyInAnyOrder("1", "2", "3")
    }

    @Test
    suspend fun `reactive persister saves entities and emits events on persist`() {
        val repository = InMemoryReactiveRepository()
        val persister = SpringDataAutomateReactivePersisterFlow<TestState, String, TestEntity, TestEvent>(
            repository, S2BatchProperties(size = 2),
        )
        val events = persister.persist(flowOf(transitionContext("1"), transitionContext("2"))).toList()
        assertThat(events.map { it.id }).containsExactly("1", "2")
        assertThat(repository.store.keys).containsExactlyInAnyOrder("1", "2")
    }

    @Test
    suspend fun `reactive persister loads by id and by ids`() {
        val repository = InMemoryReactiveRepository(mapOf("1" to TestEntity("1")))
        val persister = SpringDataAutomateReactivePersisterFlow<TestState, String, TestEntity, TestEvent>(
            repository, S2BatchProperties(),
        )
        assertThat(persister.load(automateContext, "1")).isEqualTo(TestEntity("1"))
        assertThat(persister.load(automateContext, flowOf("1")).toList()).containsExactly(TestEntity("1"))
    }
}
