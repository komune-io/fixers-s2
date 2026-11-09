package s2.sourcing.dsl.snap

interface SnapRepository<ENTITY, ID : Any> {
	suspend fun get(id: ID): ENTITY?
	suspend fun save(entity: ENTITY & Any): ENTITY
	suspend fun remove(id: ID): Boolean
}
