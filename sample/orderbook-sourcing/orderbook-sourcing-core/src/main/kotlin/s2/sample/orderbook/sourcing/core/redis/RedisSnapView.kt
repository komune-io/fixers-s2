package s2.sample.orderbook.sourcing.core.redis

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.redis.lettucemod.api.StatefulRedisModulesConnection
import com.redis.lettucemod.search.CreateOptions
import com.redis.lettucemod.search.Field
import com.redis.lettucemod.search.Language
import com.redis.lettucemod.search.SearchOptions
import com.redis.lettucemod.search.TextField
import f2.dsl.cqrs.page.OffsetPagination
import f2.dsl.cqrs.page.PageQueryResult
import io.lettuce.core.json.DefaultJsonParser
import io.lettuce.core.json.JsonPath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.apache.commons.pool2.impl.GenericObjectPool
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class RedisSnapView(
	val searchConnection: GenericObjectPool<StatefulRedisModulesConnection<String, String>>,
	val objectMapper: ObjectMapper
) {
	val logger = LoggerFactory.getLogger(RedisSnapView::class.java)!!
	companion object {
		const val TYPE = "type"
	}

	final suspend inline fun <reified MODEL> get(id: String): MODEL? =
		searchConnection.withConnection { conn ->
			val reactive = conn.reactive()
			reactive.jsonGet(buildId<MODEL>(id))
				.awaitFirstOrNull()?.let { value ->
					if (value.isNull) null else objectMapper.readValue<MODEL>(value.toString())
				}
		}

	final suspend inline fun <reified MODEL> delete(id: String): Boolean =
		searchConnection.withConnection { conn ->
			conn.reactive().jsonDel(buildId<MODEL>(id)).awaitSingleOrNull()
			true
		}

	final inline fun <reified MODEL> buildId(value: String) = "${'$'}{MODEL::class.simpleName}-${'$'}value"

	final suspend inline fun <reified MODEL> save(id: String, entity: MODEL): MODEL =
		searchConnection.withConnection { conn ->
			val reactive = conn.reactive()
			val json = objectMapper.valueToTree<ObjectNode>(entity).apply {
				put(TYPE, MODEL::class.simpleName)
			}.toString()
			val value = DefaultJsonParser().createJsonValue(json)
			reactive.jsonSet(buildId<MODEL>(id), JsonPath.of("$"), value).awaitSingle()
			// Re-read using the same borrowed connection
			reactive.jsonGet(buildId<MODEL>(id))
				.awaitFirstOrNull()
				?.let { v -> if (v.isNull) null else objectMapper.readValue<MODEL>(v.toString()) }
				?: error("JSON not found after save")
		}

	final suspend inline fun <reified MODEL> dropIndex() =
		searchConnection.withConnection { conn ->
			val connection = conn.reactive()
			val indexName = MODEL::class.simpleName
			try {
				connection.ftCreate(indexName).awaitFirstOrNull()
				connection.ftDropindex(indexName).awaitFirstOrNull()
			} catch (e: Exception) {
				logger.debug("Index[${'$'}{indexName}] nothing to drop", e)
			}
		}

	final suspend inline fun <reified MODEL> createIndex(vararg fields: RedisIndexField) =
		searchConnection.withConnection { conn ->
			val connection = conn.reactive()
			val indexName = MODEL::class.simpleName
			try {
				connection.ftInfo(indexName).awaitFirstOrNull()
			} catch (e: Exception) {
				logger.debug("Index[${'$'}{indexName}] Error during the creation", e)
				val opt = CreateOptions.builder<String, String>()
					.on(CreateOptions.DataType.JSON)
					.prefix(indexName)
					.defaultLanguage(Language.FRENCH)
					.build()
				val field = fields.map { index ->
					val asName = index.name ?: index.field
					when (index.type) {
						Field.Type.TEXT -> Field.text("\\$.${'$'}{index.field}")
							.`as`(asName)
							.noStem()
							.sortable()
							.matcher(TextField.PhoneticMatcher.FRENCH)
							.build()
						Field.Type.GEO -> Field.geo("\\$.${'$'}{index.field}").`as`(asName).build()
						Field.Type.NUMERIC -> Field.numeric("\\$.${'$'}{index.field}").`as`(asName).sortable().build()
						Field.Type.TAG -> Field.tag("\\$.${'$'}{index.field}").`as`(asName).build()
						Field.Type.VECTOR -> Field.tag("\\$.${'$'}{index.field}").`as`(asName).build()
					}
				}
				connection.ftCreate(
					indexName,
					opt,
					*field.toTypedArray(),
				).awaitFirstOrNull()
			}
		}

	final suspend inline fun <reified MODEL> createIndex() {
		val fields = MODEL::class.java.declaredFields
			.filter { it.name != "Companion" }
			.filter { it.type == String::class.java }
			.map { field -> RedisIndexField(field.name, Field.Type.TEXT) }
		createIndex<MODEL>(*fields.toTypedArray())
	}

	final suspend inline fun <reified MODEL> searchById(field: String, id: String): PageQueryResult<MODEL> =
		searchConnection.withConnection { conn ->
			val connection = conn.reactive()
			val searchOptions = SearchOptions.builder<String, String>()
			val queryByTag = id(field, id)

			val searchResult = connection.ftSearch(MODEL::class.simpleName, queryByTag, searchOptions.build()).awaitSingle()
			val results = searchResult.mapNotNull { document ->
				document["$"]?.let { objectMapper.readValue<MODEL>(it) }
			}
			PageQueryResult(
				total = searchResult.count.toInt(),
				items = results,
				pagination = OffsetPagination(
					limit = 0,
					offset = searchResult.count.toInt(),
				)
			)
		}

	final suspend inline fun <reified MODEL : Any> search(
		query: String?,
		pagination: OffsetPagination?,
		sortBy: String?
	): PageQueryResult<MODEL> = searchConnection.withConnection { conn ->
		val connection = conn.reactive()
		val searchOptions = SearchOptions.builder<String, String>()

		val pp = pagination ?: OffsetPagination(offset = 0, limit = 10000)
		pp.let {
			SearchOptions.builder<String, String>()
			searchOptions.limit(SearchOptions.limit(pp.offset.toLong(), pp.limit.toLong()))
		}
		sortBy?.let {
			searchOptions.sortBy(SearchOptions.SortBy.asc(sortBy))
		}
		val queryWithType = query?.trimToNull() ?: "*"

		val searchResult = connection.ftSearch(MODEL::class.simpleName, queryWithType, searchOptions.build()).awaitSingle()
		val results = searchResult.mapNotNull { document ->
			document["$"]?.let { objectMapper.readValue<MODEL>(it) }
		}
		PageQueryResult(
			total = searchResult.count.toInt(),
			items = results,
			pagination = pagination ?: OffsetPagination(
				limit = 0,
				offset = searchResult.count.toInt(),
			)
		)
	}

	final suspend inline fun <reified MODEL> count(): Long =
		searchConnection.withConnection { conn ->
			conn.reactive().ftSearch(MODEL::class.simpleName, "*").map { it.count }.awaitSingle()
		}

	final suspend inline fun <reified MODEL> all(): Flow<MODEL> =
		searchConnection.withConnection { conn ->
			val connection = conn.reactive()
			val searchResult = connection.ftSearch(MODEL::class.simpleName, "*").awaitSingle()
			searchResult.mapNotNull { document ->
				document["$"]?.let { objectMapper.readValue<MODEL>(it) }
			}.asFlow()
		}

	fun String.trimToNull() = if (this.trim() == "") null else this
}

class RedisIndexField(
	val field: String,
	val type: Field.Type,
	val name: String? = null,
)

suspend inline fun <T> GenericObjectPool<StatefulRedisModulesConnection<String, String>>.withConnection(
	crossinline block: suspend (StatefulRedisModulesConnection<String, String>) -> T
): T {
	val conn = borrowObject()
	var success = false
	try {
		val result = block(conn)
		success = true
		return result
	} catch (e: Exception) {
		runCatching { invalidateObject(conn) }
		throw e
	} finally {
		if (success) {
			runCatching { returnObject(conn) }
		}
	}
}

fun id(field: String, id: String): String {
	return "@${field}:{${id.replace("-", "\\-")}}"
}

fun String?.addWildcard(): String {
	val queryBuilder1 = this ?: ""
	return queryBuilder1
		.split(" ")
		.filter { it.isNotBlank() }
		.joinToString(" ") { "$it*" }
}
