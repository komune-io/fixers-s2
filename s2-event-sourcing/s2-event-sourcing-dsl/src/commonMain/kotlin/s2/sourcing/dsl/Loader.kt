package s2.sourcing.dsl

import kotlinx.coroutines.flow.Flow

interface Loader<EVENT: Any, ENTITY, ID : Any> {
	suspend fun load(id: ID): ENTITY?
	suspend fun load(events: Flow<EVENT>): ENTITY?
	suspend fun loadAndEvolve(id: ID, news: Flow<EVENT>): ENTITY?
	suspend fun evolve(events: Flow<EVENT>, entity: ENTITY? = null): ENTITY?
	suspend fun reloadHistory(): List<ENTITY>
}
