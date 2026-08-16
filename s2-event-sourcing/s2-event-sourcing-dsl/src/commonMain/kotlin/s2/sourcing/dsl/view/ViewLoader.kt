package s2.sourcing.dsl.view

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import s2.dsl.automate.Evt
import s2.dsl.automate.model.WithS2Id
import s2.sourcing.dsl.Loader
import s2.sourcing.dsl.event.EventRepository

open class ViewLoader<EVENT, ENTITY, ID>(
	private val eventRepository: EventRepository<EVENT, ID>,
	private val view: View<EVENT, ENTITY>
): Loader<EVENT, ENTITY, ID> where
EVENT: Evt,
EVENT: WithS2Id<ID> {

	override suspend fun load(id: ID & Any): ENTITY? {
		val events = eventRepository.load(id)
		return load(events)
	}

	override suspend fun load(events: Flow<EVENT>): ENTITY? {
		return evolve(events, null)
	}

	override suspend fun loadAndEvolve(id: ID & Any, news: Flow<EVENT>): ENTITY? {
		return eventRepository.load(id).let { events ->
			load(events)
		}.let { entity ->
			evolve(news, entity)
		}
	}

	override suspend fun evolve(events: Flow<EVENT>, entity: ENTITY?): ENTITY? {
		return events.fold(entity) { updated, event ->
			view.evolve(event, updated)
		}
	}

	override suspend fun reloadHistory(): List<ENTITY> {
		return eventRepository.loadAll()
			.groupByOrdered(
				{ event -> event.s2Id() },
				{ a, b ->
					when {
						a is Comparable<*> && b is Comparable<*> && a::class == b::class -> {
							@Suppress("UNCHECKED_CAST")
							(a as Comparable<Any>).compareTo(b)
						}

						else -> 0
					}
				}
			)
			.values.asFlow()
			.mapNotNull { events -> load(events.asFlow()) }
			.toList()
	}

	/**
	 * Groups the flow's elements by key, keys in first-seen order, each group sorted with
	 * [comparator] (a stable sort: elements the comparator ties keep their emission order).
	 */
	private suspend fun <T, K> Flow<T>.groupByOrdered(
		keySelector: suspend (T) -> K,
		comparator: Comparator<T>,
	): Map<K, List<T>> {
		val groups = linkedMapOf<K, MutableList<T>>()
		collect { value ->
			groups.getOrPut(keySelector(value)) { mutableListOf() }.add(value)
		}
		return groups.mapValues { (_, values) -> values.sortedWith(comparator) }
	}

}
