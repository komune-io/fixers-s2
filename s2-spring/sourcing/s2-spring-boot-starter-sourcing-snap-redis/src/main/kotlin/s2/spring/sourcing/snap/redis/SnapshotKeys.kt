package s2.spring.sourcing.snap.redis

import io.lettuce.core.ScanArgs
import io.lettuce.core.ScanStream
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.reactive.RedisReactiveCommands
import io.lettuce.core.json.JsonPath
import io.lettuce.core.json.JsonValue
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import reactor.core.publisher.Flux
import tools.jackson.core.JacksonException
import tools.jackson.databind.ObjectMapper

/**
 * The keys `<prefix>-…` of one view, read and deleted by batch, and their snapshots decoded strictly: one that no
 * reader can decode fails the read and names its key.
 */
internal class SnapshotKeys<MODEL : Any>(
    private val connection: StatefulRedisConnection<String, String>,
    private val prefix: String,
    private val model: KClass<MODEL>,
    private val objectMapper: ObjectMapper,
) {
    private val commands: RedisReactiveCommands<String, String>
        get() = connection.commands(RedisReactiveCommands.factory())

    fun key(id: String) = "$prefix-$id"

    /** Every key of the view, in scan steps; the dash keeps a model whose name starts with this one's out. */
    fun scan(): Flux<String> = ScanStream.scan(commands, ScanArgs.Builder.matches(key("*")).limit(STORE_BATCH.toLong()))

    /** [scan] in batches of [STORE_BATCH] keys. */
    fun batches(): Flow<List<String>> = scan().buffer(STORE_BATCH).asFlow()

    /** One `JSON.MGET` of [keys], one value per key in their order, or a failure: never a short answer. */
    // Lettuce takes keys as Java varargs only; a batch holds at most STORE_BATCH keys.
    @Suppress("SpreadOperator")
    suspend fun read(keys: List<String>): List<JsonValue> {
        if (keys.isEmpty()) return emptyList()
        val values = commands.jsonMGet(JsonPath.ROOT_PATH, *keys.toTypedArray()).collectList().awaitSingle()
        check(values.size == keys.size) { "JSON.MGET answered ${values.size} values for ${keys.size} keys" }
        return values
    }

    /** One `UNLINK` of [keys]: how many there were. */
    // Lettuce takes keys as Java varargs only; a batch holds at most STORE_BATCH keys.
    @Suppress("SpreadOperator")
    suspend fun unlink(keys: List<String>): Long = commands.unlink(*keys.toTypedArray()).awaitSingle()

    fun decode(key: String, json: String): MODEL =
        try {
            objectMapper.readValue(json, model.java)
        } catch (e: JacksonException) {
            throw IllegalStateException("Snapshot [$key] is not a readable $prefix", e)
        }
}

/**
 * The document of one key in a `JSON.MGET … $` answer: an array holding the root document, or a
 * nil for a missing key. A bare object is taken as the document itself.
 */
internal fun JsonValue.rootDocument(): String? = when {
    isNull -> null
    isJsonArray -> asJsonArray().takeIf { it.size() > 0 }?.get(0)?.takeUnless { it.isNull }?.toString()
    else -> toString()
}
