package s2.sourcing.dsl.snap

interface SnapRepository<ENTITY, ID> {
	suspend fun get(id: ID & Any): ENTITY?
	suspend fun save(entity: ENTITY & Any): ENTITY
	suspend fun remove(id: ID & Any): Boolean
}
