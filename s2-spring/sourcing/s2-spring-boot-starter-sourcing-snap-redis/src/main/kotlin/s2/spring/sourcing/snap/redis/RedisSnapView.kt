package s2.spring.sourcing.snap.redis

import com.fasterxml.jackson.annotation.JsonInclude
import f2.dsl.cqrs.page.OffsetPagination
import f2.dsl.cqrs.page.PageQueryResult
import io.lettuce.core.RedisCommandExecutionException
import io.lettuce.core.ScanArgs
import io.lettuce.core.ScanStream
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.reactive.RedisReactiveCommands
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.json.JsonPath
import io.lettuce.core.json.JsonValue
import io.lettuce.core.output.ComplexDataParser
import io.lettuce.core.output.ComplexOutput
import io.lettuce.core.output.StatusOutput
import io.lettuce.core.protocol.CommandArgs
import io.lettuce.core.protocol.ProtocolKeyword
import io.lettuce.core.search.SearchReply
import io.lettuce.core.search.arguments.SearchArgs
import io.lettuce.core.search.arguments.SortByArgs
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import tools.jackson.core.JacksonException
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ObjectNode
import tools.jackson.module.kotlin.kotlinModule
import tools.jackson.module.kotlin.readValue

/**
 * Snapshots of one or more models, each stored as a JSON document under `<Model>-<id>` and searched through a
 * RediSearch index named after the model. Every read is strict: a batch answered short fails, a snapshot no
 * reader can decode stops a whole-store read, and a search asked for every row refuses rather than cut.
 */
class RedisSnapView(
	val searchConnection: StatefulRedisConnection<String, String>,
	val settings: IndexSettings,
	val objectMapper: ObjectMapper = defaultObjectMapper(),
) {
	/** The reactive commands of [searchConnection], cached by the connection. */
	@PublishedApi
	internal val commands: RedisReactiveCommands<String, String>
		get() = searchConnection.commands(RedisReactiveCommands.factory())

	companion object {
		const val TYPE = "type"

		private val logger = LoggerFactory.getLogger(RedisSnapView::class.java)

		fun defaultObjectMapper(): JsonMapper = JsonMapper.builder()
			.addModule(kotlinModule())
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.changeDefaultPropertyInclusion { it.withValueInclusion(JsonInclude.Include.NON_NULL) }
			.build()
	}

	suspend inline fun <reified MODEL> get(id: String): MODEL? =
		commands.jsonGet(buildId<MODEL>(id))
			.awaitFirstOrNull()?.let { value ->
				if (value.isNull) null else objectMapper.readValue<MODEL>(value.toString())
			}

	/**
	 * Reads many snapshots in one `JSON.MGET`, with the semantics of [get] for each id: an id
	 * with no stored document is absent from the result. Independent of the search index.
	 */
	suspend inline fun <reified MODEL> getAll(ids: Collection<String>): Map<String, MODEL> {
		val distinct = ids.distinct()
		if (distinct.isEmpty()) return emptyMap()
		val values = readKeys(distinct.map { buildId<MODEL>(it) })
		return distinct.zip(values)
			.mapNotNull { (id, value) -> value.rootDocument()?.let { id to objectMapper.readValue<MODEL>(it) } }
			.toMap()
	}

	/** Every key under this model's prefix, unread: a snapshot no reader can decode goes too. */
	suspend inline fun <reified MODEL> deleteAll(): Int {
		val reactive = commands
		return storedKeys<MODEL>(reactive).chunked(STORE_BATCH).sumOf { batch -> unlinkKeys(batch) }.toInt()
	}

	@PublishedApi
	internal suspend inline fun <reified MODEL> storedKeys(reactive: RedisReactiveCommands<String, String>): List<String> {
		// The dash keeps a model whose name starts with this one's out.
		val scan = ScanArgs.Builder.matches(buildId<MODEL>("*")).limit(STORE_BATCH.toLong())
		return ScanStream.scan(reactive, scan).collectList().awaitSingle().distinct()
	}

	suspend inline fun <reified MODEL> delete(id: String): String {
		commands.jsonDel(buildId<MODEL>(id)).awaitSingleOrNull()
		return id
	}

	inline fun <reified MODEL> buildId(value: String) = "${MODEL::class.simpleName}-$value"

	suspend inline fun <reified MODEL> save(id: String, entity: MODEL): MODEL {
		val reactive = commands
		val json = objectMapper.valueToTree<ObjectNode>(entity).apply {
			put(TYPE, MODEL::class.simpleName)
		}.toString()
		// The String overload is sent as UTF-8, while the JsonValue overload encodes
		// with the JVM default charset.
		reactive.jsonSet(buildId<MODEL>(id), JsonPath.of("$"), json).awaitSingle<String>()
		return reactive.jsonGet(buildId<MODEL>(id))
			.awaitFirstOrNull()
			?.let { v -> if (v.isNull) null else objectMapper.readValue<MODEL>(v.toString()) }
			?: error("JSON not found after save")
	}

	/**
	 * Makes the index match [fields] — created when absent, kept when `FT.INFO` already describes it, dropped
	 * and re-created otherwise, its documents kept — then waits until it has read them. Within an
	 * [IndexingStart] the wait is left to the start, which waits for every index it made at once. A store that
	 * answers badly or slowly is logged, never thrown: a start does not fail on it, the searches will.
	 */
	suspend inline fun <reified MODEL> createIndex(vararg fields: RedisIndexField) {
		createIndex<MODEL>(fields.toList())
	}

	/** [createIndex] over a list of fields. */
	suspend inline fun <reified MODEL> createIndex(fields: List<RedisIndexField>) {
		val indexName = checkNotNull(MODEL::class.simpleName) { "An index needs a named model" }
		if (!make(indexName, IndexDefinition.of(fields, settings))) return
		val start = currentCoroutineContext()[IndexingStart]
		if (start != null) {
			start.made(this, indexName)
		} else {
			IndexingStart().apply { made(this@RedisSnapView, indexName) }.awaitAll()
		}
	}

	/** An index of every String property of [MODEL], as TEXT. */
	suspend inline fun <reified MODEL> createIndex() {
		val fields = MODEL::class.java.declaredFields
			.filter { it.name != "Companion" }
			.filter { it.type == String::class.java }
			.map { field -> RedisIndexField(field.name, RedisFieldType.TEXT) }
		createIndex<MODEL>(fields)
	}

	/** Makes the index match [wanted]; false when it could not be made. */
	@PublishedApi
	internal suspend fun make(indexName: String, wanted: IndexDefinition): Boolean =
		try {
			val current = indexInfo(indexName)
			val read = current?.let { IndexDefinition.read(it) }
			when {
				current == null -> create(indexName, wanted)
				read == wanted -> logger.debug("Index[${indexName}] already exists")
				else -> {
					val cause = if (read == null) "FT.INFO unreadable" else "definition differs"
					logger.info("Index[${indexName}] made again ({}); FT.INFO was {}", cause, current)
					dispatch(SearchCommand.FT_DROPINDEX, CommandArgs(StringCodec.UTF8).add(indexName))
					create(indexName, wanted)
				}
			}
			true
		} catch (e: CancellationException) {
			throw e
		} catch (e: Exception) {
			logger.error("Index[${indexName}] could not be made", e)
			false
		}

	/** Creates the index, once more if it is absent right after: the same race as « already exists ». */
	private suspend fun create(indexName: String, wanted: IndexDefinition) {
		val created = createOnce(indexName, wanted) ?: createOnce(indexName, wanted)
		val read = created?.let { IndexDefinition.read(it) }
		when {
			created == null -> logger.error("Index[${indexName}] absent once created")
			read == null -> logger.error("Index[${indexName}] FT.INFO unreadable once created: {}", created)
			read != wanted -> logger.error("Index[${indexName}] does not read back as defined: {}", created)
		}
	}

	private suspend fun createOnce(indexName: String, wanted: IndexDefinition): Map<String, Any?>? {
		try {
			dispatch(SearchCommand.FT_CREATE, wanted.createArgs(indexName))
		} catch (e: RedisCommandExecutionException) {
			// Another instance created it first: what it holds is compared like any other.
			if (!e.isIndexAlreadyPresent()) throw e
			logger.debug("Index[${indexName}] already exists", e)
		}
		return indexInfo(indexName)
	}

	/** `FT.INFO`, as plain maps, lists and scalars; null when there is no such index. */
	internal suspend fun indexInfo(indexName: String): Map<String, Any?>? =
		try {
			val output = ComplexOutput<String, String, Any?>(StringCodec.UTF8, ComplexDataParser<Any?> { plain(it) })
			val reply = commands
				.dispatch<Any?>(SearchCommand.FT_INFO, output, CommandArgs<String, String>(StringCodec.UTF8).add(indexName))
				.awaitFirstOrNull()
			asFields(reply)
		} catch (e: RedisCommandExecutionException) {
			if (e.isIndexAbsent()) null else throw e
		}

	/**
	 * Whether the index has read the stored documents, asked now; an error is logged and counts as an answer,
	 * so that a failing store ends the wait instead of being polled until the deadline.
	 */
	internal suspend fun indexed(indexName: String): Boolean =
		try {
			// RESP3 answers `indexing` as 0.0, RESP2 as "0".
			indexInfo(indexName)?.get("indexing")?.toString()?.toDoubleOrNull() == 0.0
		} catch (e: CancellationException) {
			throw e
		} catch (e: Exception) {
			logger.error("Index[${indexName}] could not be waited for", e)
			true
		}

	/**
	 * One page of the documents [query] matches, sorted by [sortBy] when given. A null [pagination] asks for
	 * every row: refused with [WholeReadRefused] past [SEARCH_WINDOW] or on a short read, never cut.
	 */
	suspend inline fun <reified MODEL : Any> search(
		query: String?,
		pagination: OffsetPagination?,
		sortBy: String?,
		descending: Boolean = false,
	): PageQueryResult<MODEL> {
		val indexName = checkNotNull(MODEL::class.simpleName) { "A search needs a named model" }
		val searchArgs = SearchArgs.builder<String>()
		val pp = pagination ?: OffsetPagination(offset = 0, limit = SEARCH_WINDOW)
		searchArgs.limit(pp.offset.toLong(), pp.limit.toLong())
		sortBy?.let {
			val order = SortByArgs.builder().attribute(sortBy)
			searchArgs.sortBy((if (descending) order.descending() else order).build())
		}
		val searchResult = commands
			.ftSearch(indexName, query.orWildcard(), searchArgs.build())
			.awaitSingle()
		return searchPage(indexName, searchResult.count, searchResult.documents(), pagination) {
			objectMapper.readValue<MODEL>(it)
		}
	}

	/** Documents the index holds: a document it could not index is not counted, see [stored]. */
	suspend inline fun <reified MODEL> count(): Long {
		val indexName = checkNotNull(MODEL::class.simpleName) { "A count needs a named model" }
		return commands.ftSearch(indexName, "*").map { it.count }.awaitSingle()
	}

	suspend inline fun <reified MODEL> all(): Flow<MODEL> = stored<MODEL>().asFlow()

	/** Every document under this model's key prefix, read by key: see [RedisSnapRepository.stored]. */
	suspend inline fun <reified MODEL> stored(): List<MODEL> {
		val reactive = commands
		return storedKeys<MODEL>(reactive).chunked(STORE_BATCH).flatMap { batch ->
			batch.zip(readKeys(batch)).mapNotNull { (key, value) ->
				value.rootDocument()?.let { document ->
					// One unreadable snapshot stops the whole read rather than leave it out.
					try {
						objectMapper.readValue<MODEL>(document)
					} catch (e: JacksonException) {
						throw IllegalStateException("Snapshot [$key] is not a readable ${MODEL::class.simpleName}", e)
					}
				}
			}
		}
	}

	/** One `JSON.MGET` of [keys], one value per key in their order, or a failure: never a short answer. */
	// Lettuce takes keys as Java varargs only; a batch holds at most STORE_BATCH keys.
	@Suppress("SpreadOperator")
	@PublishedApi
	internal suspend fun readKeys(keys: List<String>): List<JsonValue> {
		val values = commands.jsonMGet(JsonPath.ROOT_PATH, *keys.toTypedArray()).collectList().awaitSingle()
		check(values.size == keys.size) { "JSON.MGET answered ${values.size} values for ${keys.size} keys" }
		return values
	}

	/** One `UNLINK` of [keys]: how many there were. */
	// Lettuce takes keys as Java varargs only; a batch holds at most STORE_BATCH keys.
	@Suppress("SpreadOperator")
	@PublishedApi
	internal suspend fun unlinkKeys(keys: List<String>): Long = commands.unlink(*keys.toTypedArray()).awaitSingle()

	/** A module command, sent like every other: a blocking call could wait on the thread that reads its answer. */
	internal suspend fun dispatch(command: ProtocolKeyword, args: CommandArgs<String, String>) {
		commands.dispatch<String>(command, StatusOutput(StringCodec.UTF8), args).awaitFirstOrNull()
	}
}

internal object SearchCommand {
	val FT_CREATE = cmd("FT.CREATE")
	val FT_DROPINDEX = cmd("FT.DROPINDEX")
	val FT_INFO = cmd("FT.INFO")

	private fun cmd(name: String): ProtocolKeyword {
		val bytes = name.toByteArray(Charsets.US_ASCII)
		return object : ProtocolKeyword {
			override fun getBytes() = bytes

			@Deprecated("Deprecated in ProtocolKeyword")
			override fun name() = name
		}
	}
}

/** A blank query matches every document. */
@PublishedApi
internal fun String?.orWildcard(): String = this?.takeIf { it.isNotBlank() } ?: "*"

/**
 * The document of one key in a `JSON.MGET … $` answer: an array holding the root document, or a
 * nil for a missing key. A bare object is taken as the document itself.
 */
@PublishedApi
internal fun JsonValue.rootDocument(): String? = when {
	isNull -> null
	isJsonArray -> asJsonArray().takeIf { it.size() > 0 }?.get(0)?.takeUnless { it.isNull }?.toString()
	else -> toString()
}

@PublishedApi
internal class SearchDocuments(val documents: List<String>, val missing: List<String>)

@PublishedApi
internal fun SearchReply<String>.documents(): SearchDocuments {
	val (present, absent) = results.partition { it.fields.containsKey("$") }
	return SearchDocuments(present.map { it.fields.getValue("$").asString(Charsets.UTF_8) }, absent.map { it.id })
}

internal fun Throwable.isIndexAlreadyPresent(): Boolean =
	this is RedisCommandExecutionException && message.orEmpty().contains("Index already exists", ignoreCase = true)

internal fun Throwable.isIndexAbsent(): Boolean =
	this is RedisCommandExecutionException && message.orEmpty().let {
		it.contains("Unknown index", ignoreCase = true) || it.contains("no such index", ignoreCase = true)
	}

private val pageLogger = LoggerFactory.getLogger(RedisSnapView::class.java)

/**
 * A search answer as a page. A hit without a document is left out, and taken off the total too: a caller
 * comparing the rows to the total would otherwise take it for a short read.
 */
@PublishedApi
internal fun <MODEL> searchPage(
	indexName: String,
	count: Long,
	read: SearchDocuments,
	pagination: OffsetPagination?,
	decode: (String) -> MODEL,
): PageQueryResult<MODEL> {
	read.missing.forEach { key -> pageLogger.warn("Search hit [{}] carries no document; left out of the answer", key) }
	val total = count.toInt() - read.missing.size
	val results = read.documents.map(decode)
	return PageQueryResult(
		total = total,
		items = if (pagination == null) whole(indexName, total, read.missing.size, results) else results,
		pagination = pagination ?: OffsetPagination(limit = 0, offset = total),
	)
}

/**
 * Every row, or a refusal. The [total] is already without the [missing] hits, which still took their place
 * in the window.
 */
private fun <MODEL> whole(indexName: String, total: Int, missing: Int, read: List<MODEL>): List<MODEL> {
	val bound = SEARCH_WINDOW - missing
	if (total > bound) throw WholeReadRefused.TooManyEntries(indexName, total, bound)
	if (read.size < total) throw WholeReadRefused.ShortRead(indexName, total, read.size)
	return read
}
