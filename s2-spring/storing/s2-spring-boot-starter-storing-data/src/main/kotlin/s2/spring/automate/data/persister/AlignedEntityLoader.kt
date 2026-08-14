package s2.spring.automate.data.persister

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import s2.dsl.automate.model.WithS2Id

/**
 * Loads entities by id while keeping the result aligned with what was requested.
 *
 * A repository `findAllById` silently drops the ids it cannot find, which makes the
 * corresponding commands vanish without event and without error. This loader instead emits
 * one element per requested id, in the requested order, and `null` when the repository has
 * no entity for that id.
 *
 * The blocking, coroutine and reactive persisters differ only in how they reach their
 * repository, so they each supply their own [findAllById] and share the alignment logic.
 *
 * @param findAllById fetches the entities the repository knows about, in any order.
 */
internal class AlignedEntityLoader<ID : Any, ENTITY : WithS2Id<ID>>(
	private val findAllById: suspend (List<ID>) -> List<ENTITY>,
) {

	suspend fun load(id: ID): ENTITY? = load(flowOf(id)).firstOrNull()

	fun load(ids: Flow<ID>): Flow<ENTITY?> = flow {
		val requestedIds = ids.toList()
		if (requestedIds.isEmpty()) return@flow
		val loaded = findAllById(requestedIds).associateBy { it.s2Id() }
		requestedIds.forEach { id -> emit(loaded[id]) }
	}
}
