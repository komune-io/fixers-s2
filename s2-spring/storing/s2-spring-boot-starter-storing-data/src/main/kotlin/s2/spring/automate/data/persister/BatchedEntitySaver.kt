package s2.spring.automate.data.persister

import f2.dsl.fnc.operators.Batch
import f2.dsl.fnc.operators.batch
import kotlinx.coroutines.flow.Flow

/**
 * Saves each batch of entities in a single [saveAll] call, then emits the matching events in order.
 *
 * The coroutine and reactive persisters differ only in how they reach their repository, so they
 * each supply their own [saveAll] and share the batching logic.
 */
internal fun <CONTEXT, ENTITY, EVENT> Flow<CONTEXT>.saveAllBatched(
	batchParams: Batch,
	entityOf: (CONTEXT) -> ENTITY,
	eventOf: (CONTEXT) -> EVENT,
	saveAll: suspend (List<ENTITY>) -> Unit,
): Flow<EVENT> = batch(batchParams) { contexts ->
	val entities = contexts.map(entityOf)
	val events = contexts.map(eventOf)
	saveAll(entities)
	events
}
