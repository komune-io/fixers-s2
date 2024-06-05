package s2.spring.sourcing.data.event

import java.util.UUID
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import s2.dsl.automate.Evt
import s2.dsl.automate.model.WithS2Id
import s2.sourcing.dsl.event.EventRepository

class EventPersisterData<EVENT, ID>(
	private val eventRepository: SpringDataEventRepository<EVENT, ID>,
	private val eventType: KClass<EVENT>
) : EventRepository<EVENT, ID> where
EVENT: Evt,
EVENT: WithS2Id<ID>
{

	internal lateinit var json: Json

	override suspend fun load(id: ID): Flow<EVENT> {
		return eventRepository.findAllByObjId(id).toEvents()
	}

	override suspend fun loadAll(): Flow<EVENT> {
		return eventRepository.findAll().toEvents()
	}

	@OptIn(InternalSerializationApi::class)
	override suspend fun persist(event: EVENT): EVENT {
		val encoded: String=  json.encodeToString(eventType.serializer(), event)
		eventRepository.save(
			EventSourcing(
				id = UUID.randomUUID().toString(),
				objId = event.s2Id(),
				event = encoded
			)
		)
		return event
	}

	@OptIn(InternalSerializationApi::class)
	private fun Flow<EventSourcing<ID>>.toEvents(): Flow<EVENT> = map {
		json.decodeFromString(eventType.serializer(), it.event)
	}
}
