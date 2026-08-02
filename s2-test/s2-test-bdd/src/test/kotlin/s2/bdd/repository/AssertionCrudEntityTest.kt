package s2.bdd.repository

import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.reactivestreams.Publisher
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class AssertionCrudEntityTest {

    data class Entity(val id: String, val name: String)

    private class EntityAsserter(val entity: Entity) {
        fun hasName(name: String) = apply { assertThat(entity.name).isEqualTo(name) }
    }

    // ---- reactive ----

    private class InMemoryReactiveRepository(
        private val store: Map<String, Entity>,
    ) : ReactiveCrudRepository<Entity, String> {
        override fun findById(id: String): Mono<Entity> = Mono.justOrEmpty(store[id])
        override fun findById(id: Publisher<String>): Mono<Entity> = Mono.from(id).flatMap { findById(it) }
        override fun existsById(id: String): Mono<Boolean> = Mono.just(store.containsKey(id))
        override fun existsById(id: Publisher<String>): Mono<Boolean> = Mono.from(id).flatMap { existsById(it) }
        override fun <S : Entity> save(entity: S): Mono<S> = Mono.just(entity)
        override fun <S : Entity> saveAll(entities: Iterable<S>): Flux<S> = Flux.fromIterable(entities)
        override fun <S : Entity> saveAll(entityStream: Publisher<S>): Flux<S> = Flux.from(entityStream)
        override fun findAll(): Flux<Entity> = Flux.fromIterable(store.values)
        override fun findAllById(ids: Iterable<String>): Flux<Entity> =
            Flux.fromIterable(ids.mapNotNull { store[it] })

        override fun findAllById(idStream: Publisher<String>): Flux<Entity> =
            Flux.from(idStream).mapNotNull { store[it] }

        override fun count(): Mono<Long> = Mono.just(store.size.toLong())
        override fun deleteById(id: String): Mono<Void> = Mono.empty()
        override fun deleteById(id: Publisher<String>): Mono<Void> = Mono.empty()
        override fun delete(entity: Entity): Mono<Void> = Mono.empty()
        override fun deleteAllById(ids: Iterable<String>): Mono<Void> = Mono.empty()
        override fun deleteAll(entities: Iterable<Entity>): Mono<Void> = Mono.empty()
        override fun deleteAll(entityStream: Publisher<out Entity>): Mono<Void> = Mono.empty()
        override fun deleteAll(): Mono<Void> = Mono.empty()
    }

    private class ReactiveAssertion(
        override val repository: ReactiveCrudRepository<Entity, String>,
    ) : AssertionCrudEntity<Entity, String, EntityAsserter>() {
        override suspend fun assertThat(entity: Entity) = EntityAsserter(entity)
    }

    // ---- blocking ----

    private class InMemoryBlockingRepository(
        private val store: Map<String, Entity>,
    ) : CrudRepository<Entity, String> {
        override fun findById(id: String): Optional<Entity> = Optional.ofNullable(store[id])
        override fun existsById(id: String): Boolean = store.containsKey(id)
        override fun <S : Entity> save(entity: S): S = entity
        override fun <S : Entity> saveAll(entities: Iterable<S>): Iterable<S> = entities
        override fun findAll(): Iterable<Entity> = store.values
        override fun findAllById(ids: Iterable<String>): Iterable<Entity> = ids.mapNotNull { store[it] }
        override fun count(): Long = store.size.toLong()
        override fun deleteById(id: String) = Unit
        override fun delete(entity: Entity) = Unit
        override fun deleteAllById(ids: Iterable<String>) = Unit
        override fun deleteAll(entities: Iterable<Entity>) = Unit
        override fun deleteAll() = Unit
    }

    private class BlockingAssertion(
        override val repository: CrudRepository<Entity, String>,
    ) : AssertionBlockingCrudEntity<Entity, String, EntityAsserter>() {
        override suspend fun assertThat(entity: Entity) = EntityAsserter(entity)
    }

    private val store = mapOf("1" to Entity("1", "one"))

    @Test
    suspend fun `reactive assertion checks existence and asserts by id`() {
        val assertion = ReactiveAssertion(InMemoryReactiveRepository(store))
        assertion.exists("1")
        assertion.notExists("missing")
        assertion.assertThatId("1").hasName("one")
    }

    @Test
    suspend fun `reactive assertion fails when existence does not match`() {
        val assertion = ReactiveAssertion(InMemoryReactiveRepository(store))
        var failed = false
        try {
            assertion.exists("missing")
        } catch (expected: AssertionError) {
            failed = true
        }
        assertThat(failed).isTrue()
    }

    @Test
    suspend fun `blocking assertion checks existence and asserts by id`() {
        val assertion = BlockingAssertion(InMemoryBlockingRepository(store))
        assertion.exists("1")
        assertion.notExists("missing")
        assertion.assertThatId("1").hasName("one")
    }

    @Test
    suspend fun `blocking assertion fails when existence does not match`() {
        val assertion = BlockingAssertion(InMemoryBlockingRepository(store))
        var failed = false
        try {
            assertion.notExists("1")
        } catch (expected: AssertionError) {
            failed = true
        }
        assertThat(failed).isTrue()
    }
}
