package s2.bdd.repository

import kotlinx.coroutines.reactor.awaitSingle
import org.assertj.core.api.Assertions
import org.springframework.data.repository.reactive.ReactiveCrudRepository

abstract class AssertionCrudEntity<ENTITY: Any, ID: Any, ASSERTER>: AssertionEntity<ENTITY, ID, ASSERTER> {
    protected abstract val repository: ReactiveCrudRepository<ENTITY, ID>

    override suspend fun exists(id: ID) {
        val entity = existsById(id)
        Assertions.assertThat(entity).isTrue
    }

    override suspend fun assertThatId(id: ID): ASSERTER {
        exists(id)
        val entity = repository.findById(id).awaitSingle()
        return assertThat(entity)
    }

    override suspend fun notExists(id: ID) {
        Assertions.assertThat(existsById(id)).isFalse
    }

    private suspend fun existsById(id: ID): Boolean {
        return repository.existsById(id).awaitSingle()
    }

    abstract override suspend fun assertThat(entity: ENTITY): ASSERTER
}
