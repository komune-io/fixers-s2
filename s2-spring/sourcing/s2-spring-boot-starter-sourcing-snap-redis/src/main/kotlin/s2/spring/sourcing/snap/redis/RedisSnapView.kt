package s2.spring.sourcing.snap.redis

import com.fasterxml.jackson.annotation.JsonInclude
import f2.dsl.cqrs.page.OffsetPagination
import f2.dsl.cqrs.page.PageQueryResult
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.reactive.RedisReactiveCommands
import io.lettuce.core.json.JsonPath
import io.lettuce.core.search.SearchReply
import io.lettuce.core.search.arguments.SearchArgs
import io.lettuce.core.search.arguments.SortByArgs
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import s2.sourcing.dsl.snap.SnapRepository
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
    connection: StatefulRedisConnection<String, String>,
    settings: IndexSettings,
    model: KClass<MODEL>,
    fields: List<RedisIndexField>,
    private val idOf: (MODEL) -> String,
    private val objectMapper: ObjectMapper = defaultObjectMapper(),
) : SnapRepository<MODEL, String>, RedisSnapRepository<MODEL>, RedisIndexInitializer {

    final override val indexName: String = checkNotNull(model.simpleName) { "A snapshot model needs a name: $model" }

    internal val index = RedisIndex(
        connection = connection,
        name = indexName,
        definition = IndexDefinition.from(indexName, fields, settings),
    )

    private val keys = SnapshotKeys(
        connection = connection,
        prefix = indexName,
        model = model,
        objectMapper = objectMapper,
    )

    // The connection caches its reactive commands, so one fetched here stays the connection's.
    private val commands: RedisReactiveCommands<String, String> = connection.commands(RedisReactiveCommands.factory())

    /** The key of the snapshot [id]: `<Model>-<id>`. */
    fun key(id: String) = keys.key(id)

    /** The snapshot stored under [id], or null. */
    override suspend fun get(id: String): MODEL? =
        commands.jsonGet(key(id))
            .awaitFirstOrNull()
            ?.takeUnless { it.isNull }
            ?.let { value -> keys.decode(key = key(id), json = value.toString()) }

    /**
     * Reads many snapshots in `JSON.MGET` batches, with the semantics of [get] for each id: an id with no stored
     * document is absent from the result. Independent of the search index.
     */
    suspend fun getAll(ids: Collection<String>): Map<String, MODEL> {
        val snapshots = LinkedHashMap<String, MODEL>()
        for (batch in ids.distinct().chunked(STORE_BATCH)) {
            batch.zip(keys.read(batch.map(::key))).forEach { (id, value) ->
                value.rootDocument()?.let { document -> snapshots[id] = keys.decode(key = key(id), json = document) }
            }
        }
        return snapshots
    }

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
        keys.batches().collect { batch -> deleted += keys.unlink(batch) }
        return deleted.toInt()
    }

    override suspend fun isEmpty(): Boolean = keys.scan().next().awaitSingleOrNull() == null

    override fun stored(): Flow<MODEL> = flow {
        // A scan may visit a key twice; the keys seen are kept, never the snapshots.
        val seen = HashSet<String>()
        keys.batches().collect { batch ->
            val fresh = batch.filter(seen::add)
            fresh.zip(keys.read(fresh)).forEach { (key, value) ->
                value.rootDocument()?.let { document -> emit(keys.decode(key = key, json = document)) }
            }
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
            throw SearchRefusal.pageTooLarge(index = indexName, limit = pagination.limit, bound = SEARCH_WINDOW)
        }
        val page = pagination ?: OffsetPagination(offset = 0, limit = SEARCH_WINDOW)
        val args = SearchArgs.builder<String>().limit(page.offset.toLong(), page.limit.toLong())
        sortBy?.let { attribute ->
            val order = SortByArgs.builder().attribute(attribute)
            args.sortBy((if (descending) order.descending() else order).build())
        }
        val reply = commands.ftSearch(indexName, query.orWildcard(), args.build()).awaitSingle()
        val warning = reply.warnings.firstOrNull()
        if (warning != null) throw SearchRefusal.tooBroad(index = indexName, warning = warning)
        return searchPage(
            indexName = indexName,
            count = reply.count,
            read = reply.documents(),
            pagination = pagination,
            decode = { (key, json) -> keys.decode(key = key, json = json) },
        )
    }

    /**
     * Makes the index match its fields and settings and waits until it has read the stored documents: see
     * [RedisIndexInitializer.initIndex]. A store that cannot answer, a wait past [RedisIndex.INDEXING_CEILING] or a
     * stored document the index cannot read fails it.
     */
    override suspend fun initIndex() {
        index.init()
    }

    companion object {
        const val TYPE = "type"

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
            read.missing.forEach { key ->
                logger.warn("Search hit [{}] carries no document; left out of the answer", key)
            }
            val results = read.documents.map(decode)
            if (pagination != null) {
                return PageQueryResult(total = count.toInt(), items = results, pagination = pagination)
            }
            val total = count.toInt() - read.missing.size
            return PageQueryResult(
                total = total,
                items = everyRowOrRefusal(
                    indexName = indexName,
                    total = total,
                    missing = read.missing.size,
                    read = results,
                ),
                pagination = OffsetPagination(limit = 0, offset = total),
            )
        }
    }
}

/** A blank query matches every document. */
internal fun String?.orWildcard(): String = this?.takeIf { it.isNotBlank() } ?: "*"

/** A search's hits: each document with its key, and the keys of the hits that carried none. */
internal data class SearchDocuments(val documents: List<Pair<String, String>>, val missing: List<String>)

internal fun SearchReply<String>.documents(): SearchDocuments {
    val (present, absent) = results.partition { it.fields.containsKey("$") }
    return SearchDocuments(
        documents = present.map { it.id to it.fields.getValue("$").asString(Charsets.UTF_8) },
        missing = absent.map { it.id },
    )
}

// The total is already without the missing hits, which still took their place in the window.
private fun <MODEL> everyRowOrRefusal(indexName: String, total: Int, missing: Int, read: List<MODEL>): List<MODEL> {
    val bound = SEARCH_WINDOW - missing
    if (total > bound) throw SearchRefusal.tooManyEntries(index = indexName, total = total, bound = bound)
    if (read.size < total) throw SearchRefusal.shortRead(index = indexName, total = total, read = read.size)
    return read
}
