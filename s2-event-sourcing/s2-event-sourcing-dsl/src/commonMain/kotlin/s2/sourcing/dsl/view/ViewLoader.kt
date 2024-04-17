package s2.sourcing.dsl.view

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transform
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
		return eventRepository.load(id).let { events ->
			load(events)
		}
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
		.groupBy { event -> event.s2Id() }
		.reducePerKey(::load)
		.mapNotNull{it}
		.toList()

	// https://stackoverflow.com/a/58678049
//	private fun <T, K> Flow<T>.groupBy(getKey: (T) -> K): Flow<Pair<K, Flow<T>>> = flow {
//		val storage = mutableMapOf<K, SendChannel<T>>()
//		try {
//			collect { t ->
//				val key = getKey(t)
//				println("Handle ${key}")
//				storage.getOrPut(key) {
//					Channel<T>(32).also { emit(key to it.consumeAsFlow()) }
//				}.send(t)
//			}
//		} finally {
//			println("Handle groupBy finally")
//			storage.values.forEach { chan ->
//				try {
//					println("Handle groupBy finally ${chan}")
//					 chan.close()
//				} catch (e: Exception) {
//					println("Handle groupBy finally exception: ${e}")
//				}
//			}
//		}
//	}

	suspend fun <T, K> Flow<T>.groupBy(keySelector: suspend (T) -> K): Map<K, Flow<T>> {
		val resultMap = mutableMapOf<K, MutableList<T>>()

		transform { value ->
			val key = keySelector(value)
			val list = resultMap.getOrPut(key) { mutableListOf() }
			list.add(value)
			emit(resultMap)
		}.toList()

		return resultMap.mapValues { it.value.asFlow() }
	}

	@OptIn(FlowPreview::class)
	private fun <T, K, R> Map<K, Flow<T>>.reducePerKey(reduce: suspend (Flow<T>) -> R): Flow<R> {
		return this.values.asFlow().map { flow ->
			reduce(flow)
		}
	}
}
