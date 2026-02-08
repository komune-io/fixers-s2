package s2.sample.orderbook.sourcing.core.redis

import f2.dsl.cqrs.page.OffsetPagination
import f2.dsl.cqrs.page.PageQueryResult
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.json.DefaultJsonParser
import io.lettuce.core.json.JsonPath
import io.lettuce.core.output.StatusOutput
import io.lettuce.core.protocol.CommandArgs
import io.lettuce.core.protocol.ProtocolKeyword
import io.lettuce.core.search.arguments.SearchArgs
import io.lettuce.core.search.arguments.SortByArgs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.apache.commons.pool2.impl.GenericObjectPool
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ObjectNode
import tools.jackson.module.kotlin.readValue

@Component
class RedisSnapView(
	val searchConnection: GenericObjectPool<StatefulRedisConnection<String, String>>,
	val objectMapper: ObjectMapper
) {
	val logger = LoggerFactory.getLogger(RedisSnapView::class.java)!!
	companion object {
		const val TYPE = "type"
	}

	suspend inline fun <reified MODEL> get(id: String): MODEL? =
		searchConnection.withConnection { conn ->
			val reactive = conn.reactive()
			reactive.jsonGet(buildId<MODEL>(id))
				.awaitFirstOrNull()?.let { value ->
					if (value.isNull) null else objectMapper.readValue<MODEL>(value.toString())
				}
		}

	suspend inline fun <reified MODEL> delete(id: String): Boolean =
		searchConnection.withConnection { conn ->
			conn.reactive().jsonDel(buildId<MODEL>(id)).awaitSingleOrNull()
			true
		}

	inline fun <reified MODEL> buildId(value: String) = "${MODEL::class.simpleName}-$value"

	suspend inline fun <reified MODEL> save(id: String, entity: MODEL): MODEL =
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

	suspend inline fun <reified MODEL> dropIndex() =
		searchConnection.withConnection { conn ->
			val indexName = MODEL::class.simpleName!!
			try {
				val args = CommandArgs(StringCodec.UTF8).add(indexName)
				conn.sync().dispatch(SearchCommand.FT_DROPINDEX, StatusOutput(StringCodec.UTF8), args)
			} catch (e: Exception) {
				logger.debug("Index[${indexName}] nothing to drop", e)
			}
		}

	suspend inline fun <reified MODEL> createIndex(vararg fields: RedisIndexField) =
		searchConnection.withConnection { conn ->
			val indexName = MODEL::class.simpleName!!
			try {
				val args = CommandArgs(StringCodec.UTF8)
					.add(indexName)
					.add("ON").add("JSON")
					.add("PREFIX").add("1").add(indexName)
					.add("LANGUAGE").add("french")
					.add("SCHEMA")

				fields.forEach { index ->
					val asName = index.name ?: index.field
					val jsonPath = "$.${index.field}"
					args.add(jsonPath).add("AS").add(asName)
					when (index.type) {
						RedisFieldType.TEXT -> args.add("TEXT").add("NOSTEM")
							.add("PHONETIC").add("dm:fr").add("SORTABLE")
						RedisFieldType.GEO -> args.add("GEO")
						RedisFieldType.NUMERIC -> args.add("NUMERIC").add("SORTABLE")
						RedisFieldType.TAG -> args.add("TAG")
						RedisFieldType.VECTOR -> args.add("TAG")
					}
				}

				conn.sync().dispatch(SearchCommand.FT_CREATE, StatusOutput(StringCodec.UTF8), args)
			} catch (e: Exception) {
				logger.debug("Index[${indexName}] already exists or error during creation", e)
			}
		}

	suspend inline fun <reified MODEL> createIndex() {
		val fields = MODEL::class.java.declaredFields
			.filter { it.name != "Companion" }
			.filter { it.type == String::class.java }
			.map { field -> RedisIndexField(field.name, RedisFieldType.TEXT) }
		createIndex<MODEL>(*fields.toTypedArray())
	}

	suspend inline fun <reified MODEL> searchById(field: String, id: String): PageQueryResult<MODEL> =
		searchConnection.withConnection { conn ->
			val connection = conn.reactive()
			val queryByTag = id(field, id)

			val searchResult = connection.ftSearch(MODEL::class.simpleName!!, queryByTag).awaitSingle()
			val results = searchResult.results.mapNotNull { result ->
				result.fields["$"]?.let { objectMapper.readValue<MODEL>(it) }
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

	suspend inline fun <reified MODEL : Any> search(
		query: String?,
		pagination: OffsetPagination?,
		sortBy: String?
	): PageQueryResult<MODEL> = searchConnection.withConnection { conn ->
		val connection = conn.reactive()
		val searchArgs = SearchArgs.builder<String, String>()

		val pp = pagination ?: OffsetPagination(offset = 0, limit = 10000)
		searchArgs.limit(pp.offset.toLong(), pp.limit.toLong())
		sortBy?.let {
			searchArgs.sortBy(SortByArgs.builder<String>().attribute(sortBy).build())
		}
		val queryWithType = query?.trimToNull() ?: "*"

		val searchResult = connection.ftSearch(MODEL::class.simpleName!!, queryWithType, searchArgs.build()).awaitSingle()
		val results = searchResult.results.mapNotNull { result ->
			result.fields["$"]?.let { objectMapper.readValue<MODEL>(it) }
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

	suspend inline fun <reified MODEL> count(): Long =
		searchConnection.withConnection { conn ->
			conn.reactive().ftSearch(MODEL::class.simpleName!!, "*").map { it.count }.awaitSingle()
		}

	suspend inline fun <reified MODEL> all(): Flow<MODEL> =
		searchConnection.withConnection { conn ->
			val connection = conn.reactive()
			val searchArgs = SearchArgs.builder<String, String>().build()
			val searchResult = connection.ftSearch(MODEL::class.simpleName!!, "*", searchArgs).awaitSingle()
			searchResult.results.mapNotNull { result ->
				result.fields["$"]?.let { objectMapper.readValue<MODEL>(it) }
			}.asFlow()
		}

	fun String.trimToNull() = if (this.trim() == "") null else this
}

object SearchCommand {
	val FT_CREATE = cmd("FT.CREATE")
	val FT_DROPINDEX = cmd("FT.DROPINDEX")

	private fun cmd(name: String): ProtocolKeyword {
		val bytes = name.toByteArray(Charsets.US_ASCII)
		return object : ProtocolKeyword {
			override fun getBytes() = bytes
			@Deprecated("Deprecated in ProtocolKeyword")
			override fun name() = name
		}
	}
}

enum class RedisFieldType {
	TEXT, GEO, NUMERIC, TAG, VECTOR
}

class RedisIndexField(
	val field: String,
	val type: RedisFieldType,
	val name: String? = null,
)

suspend inline fun <T> GenericObjectPool<StatefulRedisConnection<String, String>>.withConnection(
	crossinline block: suspend (StatefulRedisConnection<String, String>) -> T
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
