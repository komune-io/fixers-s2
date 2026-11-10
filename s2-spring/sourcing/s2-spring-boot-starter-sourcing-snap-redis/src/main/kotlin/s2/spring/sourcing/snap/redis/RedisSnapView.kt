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
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import s2.sourcing.dsl.snap.SnapRepository
import tools.jackson.core.JacksonException
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ObjectNode
import tools.jackson.module.kotlin.kotlinModule

/**
 * The snapshots of one [model], each stored as a JSON document under `<Model>-<id>` and searched through a
 * RediSearch index named after the model, over [fields]. A consumer's view extends it with its own searches.
 *
 * Every read is strict: a batch answered short fails, a snapshot no reader can decode fails the read and names its
 * key, and a search that cannot answer every row it was asked refuses ([SearchRefusal]) rather than cut.
 */
open class RedisSnapView<MODEL : Any>(
	private val connection: StatefulRedisConnection<String, String>,
	settings: IndexSettings,
	private val model: KClass<MODEL>,
	fields: List<RedisIndexField>,
	private val idOf: (MODEL) -> String,
	private val objectMapper: ObjectMapper = defaultObjectMapper(),
) : SnapRepository<MODEL, String>, RedisSnapRepository<MODEL>, RedisIndexInitializer {

	final override val indexName: String = checkNotNull(model.simpleName) { "A snapshot model needs a name: $model" }

	private val definition = IndexDefinition.of(indexName, fields, settings)

	/** Where the index's [IndexDefinition.fingerprint] is kept: outside every view's key prefix. */
	private val definitionKey = "$indexName:definition"

	/** The reactive commands of [connection], cached by the connection. */
	private val commands: RedisReactiveCommands<String, String>
		get() = connection.commands(RedisReactiveCommands.factory())

	companion object {
		const val TYPE = "type"

		/** How long a view waits, at most, for its index to read the stored documents. */
		val INDEXING_CEILING: Duration = 60.seconds

		private val INDEXING_POLL = 100.milliseconds

		private val logger = LoggerFactory.getLogger(RedisSnapView::class.java)

		fun defaultObjectMapper(): JsonMapper = JsonMapper.builder()
			.addModule(kotlinModule())
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.changeDefaultPropertyInclusion { it.withValueInclusion(JsonInclude.Include.NON_NULL) }
			.build()

		/**
		 * A search answer as a page. A hit without a document is left out; on a whole read it is taken off the
		 * total too, which a caller compares to the rows.
		 */
		internal fun <MODEL> searchPage(
			indexName: String,
			count: Long,
			read: SearchDocuments,
			pagination: OffsetPagination?,
			decode: (Pair<String, String>) -> MODEL,
		): PageQueryResult<MODEL> {
			read.missing.forEach { key -> logger.warn("Search hit [{}] carries no document; left out of the answer", key) }
			val results = read.documents.map(decode)
			if (pagination != null) return PageQueryResult(total = count.toInt(), items = results, pagination = pagination)
			val total = count.toInt() - read.missing.size
			return PageQueryResult(
				total = total,
				items = whole(indexName, total, read.missing.size, results),
				pagination = OffsetPagination(limit = 0, offset = total),
			)
		}
	}

	/** The key of the snapshot [id]: `<Model>-<id>`. */
	fun key(id: String) = "$indexName-$id"

	/** The snapshot stored under [id], or null. */
	override suspend fun get(id: String): MODEL? =
		commands.jsonGet(key(id)).awaitFirstOrNull()?.takeUnless { it.isNull }?.let { decode(key(id), it.toString()) }

	/**
	 * Reads many snapshots in `JSON.MGET` batches, with the semantics of [get] for each id: an id with no stored
	 * document is absent from the result. Independent of the search index.
	 */
	suspend fun getAll(ids: Collection<String>): Map<String, MODEL> =
		ids.distinct().chunked(STORE_BATCH)
			.flatMap { batch -> batch.zip(readKeys(batch.map(::key))) }
			.mapNotNull { (id, value) -> value.rootDocument()?.let { id to decode(key(id), it) } }
			.toMap()

	/** Stores [entity] under its id, marked with its model's name, and answers it. */
	override suspend fun save(entity: MODEL): MODEL {
		val key = key(idOf(entity))
		val json = objectMapper.valueToTree<ObjectNode>(entity).apply { put(TYPE, indexName) }.toString()
		// The String overload is sent as UTF-8, while the JsonValue overload encodes with the JVM default charset.
		val answer = commands.jsonSet(key, JsonPath.of("$"), json).awaitSingle<String>()
		check(answer == "OK") { "JSON.SET of $key answered $answer" }
		return entity
	}

	/** Deletes the snapshot stored under [id]; whether there was one. */
	override suspend fun remove(id: String): Boolean = commands.jsonDel(key(id)).awaitSingle() > 0

	override suspend fun deleteAll(): Int {
		var deleted = 0L
		storedKeys().collect { batch -> deleted += unlinkKeys(batch) }
		return deleted.toInt()
	}

	override suspend fun isEmpty(): Boolean = scan().next().awaitSingleOrNull() == null

	override fun stored(): Flow<MODEL> = flow {
		// A scan may visit a key twice; the keys seen are kept, never the snapshots.
		val seen = HashSet<String>()
		storedKeys().collect { batch ->
			val keys = batch.filter(seen::add)
			keys.zip(readKeys(keys)).forEach { (key, value) -> value.rootDocument()?.let { emit(decode(key, it)) } }
		}
	}

	override suspend fun count(): Long =
		commands.ftSearch(indexName, "*", SearchArgs.builder<String>().limit(0, 0).build()).awaitSingle().count

	/**
	 * One page of the documents [query] matches, sorted by [sortBy] when given. A null [pagination] asks for every
	 * row: refused past [SEARCH_WINDOW] or on a short read, never cut. A page larger than [SEARCH_WINDOW], or a
	 * search RediSearch answers with a warning, is refused too.
	 */
	suspend fun search(
		query: String?,
		pagination: OffsetPagination?,
		sortBy: String?,
		descending: Boolean = false,
	): PageQueryResult<MODEL> {
		if (pagination != null && pagination.limit > SEARCH_WINDOW) {
			throw SearchRefusal.pageTooLarge(indexName, pagination.limit, SEARCH_WINDOW)
		}
		val page = pagination ?: OffsetPagination(offset = 0, limit = SEARCH_WINDOW)
		val args = SearchArgs.builder<String>().limit(page.offset.toLong(), page.limit.toLong())
		sortBy?.let {
			val order = SortByArgs.builder().attribute(it)
			args.sortBy((if (descending) order.descending() else order).build())
		}
		val reply = commands.ftSearch(indexName, query.orWildcard(), args.build()).awaitSingle()
		reply.warnings.firstOrNull()?.let { throw SearchRefusal.tooBroad(indexName, it) }
		return searchPage(indexName, reply.count, reply.documents(), pagination) { (key, json) -> decode(key, json) }
	}

	/**
	 * Makes the index match its definition — created when absent, kept when the definition written next to it is
	 * this one, dropped and re-created otherwise, its documents kept — then waits until it has read them. An index
	 * kept with documents it could not read is made again once; any failure left fails the start.
	 */
	override suspend fun initIndex() {
		val made = make()
		val failures = awaitIndexed().failures()
		if (failures == 0L) return
		check(!made) { "Index[$indexName]: ${unindexed(failures)}" }
		logger.info("Index[{}] made again: {}", indexName, unindexed(failures))
		remake()
		val left = awaitIndexed().failures()
		check(left == 0L) { "Index[$indexName]: ${unindexed(left)}" }
	}

	private fun unindexed(count: Long) = "$count stored documents could not be indexed"

	/** Makes the index match its definition; whether it was made now. */
	private suspend fun make(): Boolean {
		val current = indexInfo(indexName)
		val written = commands.get(definitionKey).awaitSingleOrNull()
		return when {
			current == null -> true.also { create() }
			written == definition.fingerprint -> false.also { logger.debug("Index[{}] already exists", indexName) }
			else -> true.also {
				logger.info("Index[{}] made again: its definition differs from [{}]", indexName, written)
				remake()
			}
		}
	}

	private suspend fun remake() {
		try {
			dispatch(SearchCommand.FT_DROPINDEX, CommandArgs(StringCodec.UTF8).add(indexName))
		} catch (e: RedisCommandExecutionException) {
			if (!e.isIndexAbsent()) throw e
		}
		create()
	}

	private suspend fun create() {
		try {
			dispatch(SearchCommand.FT_CREATE, definition.createArgs(indexName))
		} catch (e: RedisCommandExecutionException) {
			// Another instance of the same view, starting at the same time, created it first.
			if (!e.isIndexAlreadyPresent()) throw e
			logger.debug("Index[{}] already exists", indexName, e)
		}
		check(commands.set(definitionKey, definition.fingerprint).awaitSingle() == "OK") {
			"Index[$indexName]: its definition could not be written to $definitionKey"
		}
	}

	/** Waits until the index has read the stored documents, at most [ceiling]; answers its last `FT.INFO`. */
	internal suspend fun awaitIndexed(ceiling: Duration = INDEXING_CEILING): Map<String, Any?> =
		withTimeoutOrNull(ceiling) {
			var info = checkNotNull(indexInfo(indexName)) { "Index[$indexName] absent once made" }
			while (!info.indexed()) {
				delay(INDEXING_POLL)
				info = checkNotNull(indexInfo(indexName)) { "Index[$indexName] dropped while it was indexing" }
			}
			info
		} ?: error("Index[$indexName] still indexing after $ceiling")

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

	private fun decode(key: String, json: String): MODEL =
		try {
			objectMapper.readValue(json, model.java)
		} catch (e: JacksonException) {
			throw IllegalStateException("Snapshot [$key] is not a readable $indexName", e)
		}

	private fun scan(): Flux<String> =
		// The dash keeps a model whose name starts with this one's out.
		ScanStream.scan(commands, ScanArgs.Builder.matches(key("*")).limit(STORE_BATCH.toLong()))

	private fun storedKeys(): Flow<List<String>> = scan().buffer(STORE_BATCH).asFlow()

	/** One `JSON.MGET` of [keys], one value per key in their order, or a failure: never a short answer. */
	// Lettuce takes keys as Java varargs only; a batch holds at most STORE_BATCH keys.
	@Suppress("SpreadOperator")
	private suspend fun readKeys(keys: List<String>): List<JsonValue> {
		if (keys.isEmpty()) return emptyList()
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

/** Whether `FT.INFO` says the index has read the stored documents: RESP3 answers `indexing` as 0.0, RESP2 as "0". */
private fun Map<String, Any?>.indexed(): Boolean = get("indexing")?.toString()?.toDoubleOrNull() == 0.0

/** The stored documents `FT.INFO` says the index could not read. */
internal fun Map<String, Any?>.failures(): Long = get("hash_indexing_failures")?.toString()?.toDoubleOrNull()?.toLong()
	?: error("FT.INFO carries no hash_indexing_failures: $this")

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

/** A search's hits: each document with its key, and the keys of the hits that carried none. */
internal class SearchDocuments(val documents: List<Pair<String, String>>, val missing: List<String>)

internal fun SearchReply<String>.documents(): SearchDocuments {
	val (present, absent) = results.partition { it.fields.containsKey("$") }
	return SearchDocuments(present.map { it.id to it.fields.getValue("$").asString(Charsets.UTF_8) }, absent.map { it.id })
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
	if (total > bound) throw SearchRefusal.tooManyEntries(indexName, total, bound)
	if (read.size < total) throw SearchRefusal.shortRead(indexName, total, read.size)
	return read
}
