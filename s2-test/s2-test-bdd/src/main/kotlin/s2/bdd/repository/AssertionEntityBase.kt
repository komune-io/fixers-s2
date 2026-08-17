package s2.bdd.repository

import org.assertj.core.api.Assertions

/**
 * Shared assertion logic over two data-access primitives ([existsById], [findById]).
 * Subclasses only provide the primitives (and the entity asserter factory).
 */
abstract class AssertionEntityBase<ENTITY, ID : Any, ASSERTER> : AssertionEntity<ENTITY, ID, ASSERTER> {

    protected abstract suspend fun existsById(id: ID): Boolean
    protected abstract suspend fun findById(id: ID): ENTITY?

    override suspend fun exists(id: ID) {
        Assertions.assertThat(existsById(id)).isTrue
    }

    override suspend fun notExists(id: ID) {
        Assertions.assertThat(existsById(id)).isFalse
    }

    override suspend fun assertThatId(id: ID): ASSERTER {
        val entity = findById(id)
        Assertions.assertThat(entity).isNotNull
        return assertThat(entity!!)
    }
}
