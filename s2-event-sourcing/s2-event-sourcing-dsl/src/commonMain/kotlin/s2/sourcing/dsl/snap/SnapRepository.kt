package s2.sourcing.dsl.snap

interface SnapRepository<ENTITY : Any, ID : Any> {
	suspend fun get(id: ID): ENTITY?
	suspend fun save(entity: ENTITY): ENTITY
	suspend fun remove(id: ID): Boolean
}
