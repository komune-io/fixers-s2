package s2.bdd.repository

abstract class AssertionApiEntity<Entity, ID: Any, Asserter>: AssertionEntityBase<Entity, ID, Asserter>() {

    override suspend fun existsById(id: ID): Boolean {
        return findById(id) != null
    }

    abstract override suspend fun assertThat(entity: Entity): Asserter
    public abstract override suspend fun findById(id: ID): Entity?
}
