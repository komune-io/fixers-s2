package s2.bdd.repository

import org.springframework.data.repository.CrudRepository

abstract class AssertionBlockingCrudEntity<ENTITY: Any, ID: Any, ASSERTER>:
    AssertionEntityBase<ENTITY, ID, ASSERTER>() {
    protected abstract val repository: CrudRepository<ENTITY, ID>

    override suspend fun existsById(id: ID): Boolean = repository.existsById(id)

    override suspend fun findById(id: ID): ENTITY? = repository.findById(id).orElse(null)

    abstract override suspend fun assertThat(entity: ENTITY): ASSERTER
}
