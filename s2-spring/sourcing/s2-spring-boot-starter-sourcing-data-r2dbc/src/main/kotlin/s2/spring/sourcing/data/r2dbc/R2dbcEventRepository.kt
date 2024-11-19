package s2.spring.sourcing.data.r2dbc

import java.util.UUID
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.r2dbc.core.flow
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Mono
import s2.dsl.automate.Evt
import s2.dsl.automate.model.WithS2Id
import s2.sourcing.dsl.event.EventRepository
import s2.spring.sourcing.data.event.EventSourcing

class R2dbcEventRepository<EVENT, ID>(
	private val json: Json,
	private val databaseClient: DatabaseClient,
	private val r2dbcEntityTemplate: R2dbcEntityTemplate,
	private val eventType: KClass<EVENT>,
	private val tableName: String = "s2_event_sourcing_${eventType.simpleName!!}".lowercase()
) : EventRepository<EVENT, ID> where
EVENT: Evt,
EVENT: WithS2Id<ID>
{

	fun delete(id: Long?): Mono<Void> {
		return r2dbcEntityTemplate.delete(EventSourcing::class.java)
			.from(tableName)
			.matching(Query.query(Criteria.where("id").`is`(id!!)))
			.all()
			.then()
	}

	override suspend fun load(id: ID): Flow<EVENT> {
		return r2dbcEntityTemplate.select(EventSourcing::class.java)
			.from(tableName)
			.matching(Query.query(Criteria.where("obj_id").`is`(id!!)))
			.flow().toEvents()
	}

	override suspend fun loadAll(): Flow<EVENT> {
		return r2dbcEntityTemplate.select(EventSourcing::class.java)
			.from(tableName)
			.flow().toEvents()
	}

	@OptIn(InternalSerializationApi::class)
	override suspend fun persist(events: Flow<EVENT>): Flow<EVENT> {
		return events.map { event ->
			val encoded =  json.encodeToString(eventType.serializer(), event)
			r2dbcEntityTemplate.insert(EventSourcing::class.java)
				.into(tableName)
				.using(EventSourcing(
					id = UUID.randomUUID().toString(),
					objId = event.s2Id(),
					event = encoded
				)).toEvent().awaitSingle()
		}

	}

	override suspend fun createTable() {
		val createScript = """
			CREATE TABLE IF NOT EXISTS ${tableName} (
			      id VARCHAR(255) PRIMARY KEY,
			      obj_id VARCHAR(255) NOT NULL,
			      event text NOT NULL,
			      created_by VARCHAR(255),
			      created_date TIMESTAMP,
			      last_modified_by VARCHAR(255),
			      last_modified_date TIMESTAMP,
			      version INTEGER
			);

		""".trimIndent()

		databaseClient.sql(createScript)
			.then().awaitFirstOrNull()
	}

	@OptIn(InternalSerializationApi::class)
	override suspend fun persist(event: EVENT): EVENT {
		val encoded =  json.encodeToString(eventType.serializer(), event)
		return r2dbcEntityTemplate.insert(EventSourcing::class.java)
			.into(tableName)
			.using(EventSourcing(
				id = UUID.randomUUID().toString(),
				objId = event.s2Id(),
				event = encoded
			)).toEvent().awaitSingle()
	}

	@OptIn(InternalSerializationApi::class)
	private fun Flow<EventSourcing<*>>.toEvents(): Flow<EVENT> = map {
		json.decodeFromString(eventType.serializer(), it.event)
	}

	@OptIn(InternalSerializationApi::class)
	private fun Mono<EventSourcing<*>>.toEvent(): Mono<EVENT> = map {
		json.decodeFromString(eventType.serializer(), it.event)
	}
}
