package s2.bdd.repository

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.repository.reactive.ReactiveCrudRepository

abstract class AssertionCrudEntity<ENTITY: Any, ID: Any, ASSERTER>: AssertionEntityBase<ENTITY, ID, ASSERTER>() {
    protected abstract val repository: ReactiveCrudRepository<ENTITY, ID>

    override suspend fun existsById(id: ID): Boolean {
        return repository.existsById(id).awaitSingle()
    }

    override suspend fun findById(id: ID): ENTITY? {
        return repository.findById(id).awaitSingleOrNull()
    }

    abstract override suspend fun assertThat(entity: ENTITY): ASSERTER
}
