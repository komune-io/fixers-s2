package s2.bdd.repository

import org.assertj.core.api.Assertions
import org.springframework.data.repository.CrudRepository

abstract class AssertionBlockingCrudEntity<ENTITY: Any, ID: Any, ASSERTER>: AssertionEntity<ENTITY, ID, ASSERTER> {
    protected abstract val repository: CrudRepository<ENTITY, ID>

    override suspend fun exists(id: ID) {
        val entity = existsById(id)
        Assertions.assertThat(entity).isTrue
    }

    override suspend fun notExists(id: ID) {
        Assertions.assertThat(existsById(id)).isFalse
    }

    override suspend fun assertThatId(id: ID): ASSERTER {
        exists(id)
        val entity = repository.findById(id).get()
        return assertThat(entity)
    }

    private suspend fun existsById(id: ID): Boolean {
        return repository.existsById(id)
    }

    abstract override suspend fun assertThat(entity: ENTITY): ASSERTER
}
