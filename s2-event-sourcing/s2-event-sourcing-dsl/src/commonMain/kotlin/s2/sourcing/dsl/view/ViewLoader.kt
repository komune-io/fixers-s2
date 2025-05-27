package s2.sourcing.dsl.view

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transform
import s2.dsl.automate.Evt
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2Iteration
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

	override suspend fun reloadHistory(): List<ENTITY> = eventRepository.loadAll()
		.groupBy(
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
		.reducePerKey(::load)
		.mapNotNull{it}
		.toList()

	suspend fun <T, K> Flow<T>.groupBy(
		keySelector: suspend (T) -> K,
		comparator: Comparator<T>? = null,
	): Map<K, Flow<T>> {
		val resultMap = linkedMapOf<K, MutableList<T>>()

		transform { value ->
			val key = keySelector(value)
			val list = resultMap.getOrPut(key) { mutableListOf() }
			list.add(value)
			emit(resultMap)
		}.toList()

		return resultMap.mapValues { values ->
			if(comparator != null) {
				values.value.sortedWith(comparator).asFlow()
			} else {
				values.value.asFlow()
			}
		}
	}

	private fun <T, K, R> Map<K, Flow<T>>.reducePerKey(reduce: suspend (Flow<T>) -> R): Flow<R> {
		return this.values.asFlow().map { flow ->
			reduce(flow)
		}
	}
}
