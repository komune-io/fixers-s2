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
import java.lang.reflect.Modifier
import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KClass
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
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
	private val commands: RedisReactiveCommands<String, String>
		get() = searchConnection.commands(RedisReactiveCommands.factory())

	companion object {
		const val TYPE = "type"

		private val logger = LoggerFactory.getLogger(RedisSnapView::class.java)

		fun defaultObjectMapper(): JsonMapper = JsonMapper.builder()
			.addModule(kotlinModule())
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.changeDefaultPropertyInclusion { it.withValueInclusion(JsonInclude.Include.NON_NULL) }
			.build()

		/**
		 * A search answer as a page. A hit without a document is left out, and taken off the total too: a caller
		 * comparing the rows to the total would otherwise take it for a short read.
		 */
		internal fun <MODEL> searchPage(
			indexName: String,
			count: Long,
			read: SearchDocuments,
			pagination: OffsetPagination?,
			decode: (String) -> MODEL,
		): PageQueryResult<MODEL> {
			read.missing.forEach { key -> logger.warn("Search hit [{}] carries no document; left out of the answer", key) }
			val total = count.toInt() - read.missing.size
			val results = read.documents.map(decode)
			return PageQueryResult(
				total = total,
				items = if (pagination == null) whole(indexName, total, read.missing.size, results) else results,
				pagination = pagination ?: OffsetPagination(limit = 0, offset = total),
			)
		}
	}

	suspend inline fun <reified MODEL : Any> get(id: String): MODEL? = get(MODEL::class, id)

	/** The snapshot of [model] stored under [id], or null. */
	suspend fun <MODEL : Any> get(model: KClass<MODEL>, id: String): MODEL? =
		commands.jsonGet(buildId(model, id)).awaitFirstOrNull()
			?.takeUnless { it.isNull }
			?.let { objectMapper.readValue(it.toString(), model.java) }

	/**
	 * Reads many snapshots in one `JSON.MGET`, with the semantics of [get] for each id: an id
	 * with no stored document is absent from the result. Independent of the search index.
	 */
	suspend inline fun <reified MODEL : Any> getAll(ids: Collection<String>): Map<String, MODEL> =
		getAll(MODEL::class, ids)

	/** [getAll] of [model]. */
	suspend fun <MODEL : Any> getAll(model: KClass<MODEL>, ids: Collection<String>): Map<String, MODEL> {
		val distinct = ids.distinct()
		if (distinct.isEmpty()) return emptyMap()
		return distinct.chunked(STORE_BATCH)
			.flatMap { batch -> batch.zip(readKeys(batch.map { buildId(model, it) })) }
			.mapNotNull { (id, value) -> value.rootDocument()?.let { id to objectMapper.readValue(it, model.java) } }
			.toMap()
	}

	/** Every key under this model's prefix, unread: a snapshot no reader can decode goes too. */
	suspend inline fun <reified MODEL : Any> deleteAll(): Int = deleteAll(MODEL::class)

	/** [deleteAll] of [model]. */
	suspend fun deleteAll(model: KClass<*>): Int =
		storedKeys(model).chunked(STORE_BATCH).sumOf { batch -> unlinkKeys(batch) }.toInt()

	private suspend fun storedKeys(model: KClass<*>): List<String> {
		// The dash keeps a model whose name starts with this one's out.
		val scan = ScanArgs.Builder.matches(buildId(model, "*")).limit(STORE_BATCH.toLong())
		return ScanStream.scan(commands, scan).collectList().awaitSingle().distinct()
	}

	suspend inline fun <reified MODEL : Any> delete(id: String): String = delete(MODEL::class, id)

	/** Deletes the snapshot of [model] stored under [id]; answers [id]. */
	suspend fun delete(model: KClass<*>, id: String): String {
		commands.jsonDel(buildId(model, id)).awaitSingleOrNull()
		return id
	}

	inline fun <reified MODEL : Any> buildId(value: String) = buildId(MODEL::class, value)

	/** The key of [model]'s snapshot [value]: `<Model>-<value>`. */
	fun buildId(model: KClass<*>, value: String) = "${indexName(model)}-$value"

	suspend inline fun <reified MODEL : Any> save(id: String, entity: MODEL): MODEL = save(MODEL::class, id, entity)

	/** Stores [entity] under [id], marked with its model's name, and answers it. */
	suspend fun <MODEL : Any> save(model: KClass<MODEL>, id: String, entity: MODEL): MODEL {
		val json = objectMapper.valueToTree<ObjectNode>(entity).apply { put(TYPE, indexName(model)) }.toString()
		// The String overload is sent as UTF-8, while the JsonValue overload encodes
		// with the JVM default charset.
		val answer = commands.jsonSet(buildId(model, id), JsonPath.of("$"), json).awaitSingle<String>()
		check(answer == "OK") { "JSON.SET of ${buildId(model, id)} answered $answer" }
		return entity
	}

	/**
	 * Makes the index match [fields] — created when absent, kept when `FT.INFO` already describes it, dropped
	 * and re-created otherwise, its documents kept — then waits until it has read them. Within an
	 * [IndexingStart] the wait is left to the start, which waits for every index it made at once. A store that
	 * answers badly or slowly is logged, never thrown: a start does not fail on it, the searches will.
	 */
	suspend inline fun <reified MODEL : Any> createIndex(vararg fields: RedisIndexField) =
		createIndex(MODEL::class, fields.toList())

	/** An index of every String property of [MODEL], as TEXT. */
	suspend inline fun <reified MODEL : Any> createIndex() = createIndex(MODEL::class, textFields(MODEL::class))

	/** [createIndex] of [model] over a list of fields. */
	suspend fun createIndex(model: KClass<*>, fields: List<RedisIndexField>) {
		val indexName = indexName(model)
		if (!make(indexName, IndexDefinition.of(indexName, fields, settings))) return
		val start = currentCoroutineContext()[IndexingStart]
		if (start != null) {
			start.made(this, indexName)
		} else {
			IndexingStart().apply { made(this@RedisSnapView, indexName) }.awaitAll()
		}
	}

	/** Every String property of [model], as TEXT. */
	fun textFields(model: KClass<*>): List<RedisIndexField> = generateSequence<Class<*>>(model.java) { it.superclass }
		.flatMap { it.declaredFields.asSequence() }
		.filterNot { Modifier.isStatic(it.modifiers) }
		.distinctBy { it.name }
		.toList()
		.filter { it.type == String::class.java }
		.map { field -> RedisIndexField(field.name, RedisFieldType.TEXT) }

	/** Makes the index match [wanted]; false when it could not be made, so that no start waits for it. */
	internal suspend fun make(indexName: String, wanted: IndexDefinition): Boolean =
		try {
			val current = indexInfo(indexName)
			val read = current?.let { IndexDefinition.read(it) }
			when {
				current == null -> create(indexName, wanted)
				read == wanted -> true.also { logger.debug("Index[${indexName}] already exists") }
				else -> {
					val cause = if (read == null) "FT.INFO unreadable" else "definition differs"
					logger.info("Index[${indexName}] made again ({}); FT.INFO was {}", cause, current)
					dispatch(SearchCommand.FT_DROPINDEX, CommandArgs(StringCodec.UTF8).add(indexName))
					create(indexName, wanted)
				}
			}
		} catch (e: CancellationException) {
			throw e
		} catch (e: Exception) {
			logger.error("Index[${indexName}] could not be made", e)
			false
		}

	/**
	 * Creates the index, once more if it is absent right after: the same race as « already exists ». False when
	 * it is absent or unreadable once created: there is nothing a start could wait for.
	 */
	private suspend fun create(indexName: String, wanted: IndexDefinition): Boolean {
		val created = createOnce(indexName, wanted) ?: createOnce(indexName, wanted)
		return created(indexName, created, wanted)
	}

	/** Whether [created], the `FT.INFO` read once the index was created, shows an index a start can wait for. */
	internal fun created(indexName: String, created: Map<String, Any?>?, wanted: IndexDefinition): Boolean {
		val read = created?.let { IndexDefinition.read(it) }
		when {
			created == null -> logger.error("Index[${indexName}] absent once created")
			read == null -> logger.error("Index[${indexName}] FT.INFO unreadable once created: {}", created)
			read != wanted -> logger.error("Index[${indexName}] does not read back as defined: {}", created)
		}
		return read != null
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
	): PageQueryResult<MODEL> = search(MODEL::class, SearchRequest(query, pagination, sortBy, descending))

	/** [search] of [model]. */
	suspend fun <MODEL : Any> search(model: KClass<MODEL>, request: SearchRequest): PageQueryResult<MODEL> {
		val indexName = indexName(model)
		val searchArgs = SearchArgs.builder<String>()
		val pp = request.pagination ?: OffsetPagination(offset = 0, limit = SEARCH_WINDOW)
		searchArgs.limit(pp.offset.toLong(), pp.limit.toLong())
		request.sortBy?.let { sortBy ->
			val order = SortByArgs.builder().attribute(sortBy)
			searchArgs.sortBy((if (request.descending) order.descending() else order).build())
		}
		val searchResult = commands.ftSearch(indexName, request.query.orWildcard(), searchArgs.build()).awaitSingle()
		return searchPage(indexName, searchResult.count, searchResult.documents(), request.pagination) {
			objectMapper.readValue(it, model.java)
		}
	}

	/** Documents the index holds: a document it could not index is not counted, see [stored]. */
	suspend inline fun <reified MODEL : Any> count(): Long = count(MODEL::class)

	/** [count] of [model]. */
	suspend fun count(model: KClass<*>): Long =
		commands.ftSearch(indexName(model), "*", SearchArgs.builder<String>().limit(0, 0).build())
			.map { it.count }.awaitSingle()

	/** Snapshots stored under [MODEL]'s key prefix, counted by key: the index may hold fewer, see [stored]. */
	suspend inline fun <reified MODEL : Any> storedCount(): Long = storedCount(MODEL::class)

	/** [storedCount] of [model]. */
	suspend fun storedCount(model: KClass<*>): Long = storedKeys(model).size.toLong()

	/** Every stored snapshot, as [stored] reads them, once the flow is collected. */
	inline fun <reified MODEL : Any> all(): Flow<MODEL> = flow { emitAll(stored(MODEL::class).asFlow()) }

	/** Every document under this model's key prefix, read by key: see [RedisSnapRepository.stored]. */
	suspend inline fun <reified MODEL : Any> stored(): List<MODEL> = stored(MODEL::class)

	/** [stored] of [model]. */
	suspend fun <MODEL : Any> stored(model: KClass<MODEL>): List<MODEL> =
		storedKeys(model).chunked(STORE_BATCH).flatMap { batch ->
			batch.zip(readKeys(batch)).mapNotNull { (key, value) ->
				value.rootDocument()?.let { document ->
					// One unreadable snapshot stops the whole read rather than leave it out.
					try {
						objectMapper.readValue(document, model.java)
					} catch (e: JacksonException) {
						throw IllegalStateException("Snapshot [$key] is not a readable ${indexName(model)}", e)
					}
				}
			}
		}

	/** One `JSON.MGET` of [keys], one value per key in their order, or a failure: never a short answer. */
	// Lettuce takes keys as Java varargs only; a batch holds at most STORE_BATCH keys.
	@Suppress("SpreadOperator")
	private suspend fun readKeys(keys: List<String>): List<JsonValue> {
		val values = commands.jsonMGet(JsonPath.ROOT_PATH, *keys.toTypedArray()).collectList().awaitSingle()
		check(values.size == keys.size) { "JSON.MGET answered ${values.size} values for ${keys.size} keys" }
		return values
	}

	/** One `UNLINK` of [keys]: how many there were. */
	// Lettuce takes keys as Java varargs only; a batch holds at most STORE_BATCH keys.
	@Suppress("SpreadOperator")
	private suspend fun unlinkKeys(keys: List<String>): Long = commands.unlink(*keys.toTypedArray()).awaitSingle()

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
internal fun String?.orWildcard(): String = this?.takeIf { it.isNotBlank() } ?: "*"

/**
 * The document of one key in a `JSON.MGET … $` answer: an array holding the root document, or a
 * nil for a missing key. A bare object is taken as the document itself.
 */
internal fun JsonValue.rootDocument(): String? = when {
	isNull -> null
	isJsonArray -> asJsonArray().takeIf { it.size() > 0 }?.get(0)?.takeUnless { it.isNull }?.toString()
	else -> toString()
}

internal class SearchDocuments(val documents: List<String>, val missing: List<String>)

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

/** What one search asks: see [RedisSnapView.search]. */
class SearchRequest(
	val query: String?,
	val pagination: OffsetPagination?,
	val sortBy: String?,
	val descending: Boolean = false,
)

/** The index of [model], named after it, as its keys are prefixed. */
fun indexName(model: KClass<*>): String = checkNotNull(model.simpleName) { "A snapshot model needs a name: $model" }
