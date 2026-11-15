package s2.spring.sourcing.snap.redis

import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.reactive.RedisReactiveCommands
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.output.ValueOutput
import io.lettuce.core.protocol.CommandArgs
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tools.jackson.core.JacksonException
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ObjectNode

/**
 * Gives every stored document of a view the sort keys its text holds, without decoding a snapshot or replaying an
 * event: a document saved before the keys existed, or before an attribute was added, would sort as a missing text.
 * A key is written only if the document is still the one it was computed from, so a `save` running meanwhile is
 * never undone.
 */
internal class SortKeyBackfill(
    private val connection: StatefulRedisConnection<String, String>,
    private val indexName: String,
    private val keys: SnapshotKeys<*>,
    private val sortKeys: SortKeys,
    private val objectMapper: ObjectMapper,
) {
    private val commands: RedisReactiveCommands<String, String>
        get() = connection.commands(RedisReactiveCommands.factory())

    enum class Outcome { CURRENT, KEYED, GONE, UNKEYABLE }

    /** Keys every stored document that lacks its keys; how many it keyed. */
    suspend fun run(): Int {
        if (sortKeys.isEmpty) return 0
        var keyed = 0
        val seen = HashSet<String>()
        keys.batches().collect { batch ->
            val fresh = batch.filter(seen::add)
            fresh.zip(keys.read(fresh)).forEach { (key, value) ->
                if (!isCurrent(key, value.rootDocument()) && key(key) == Outcome.KEYED) keyed++
            }
        }
        if (keyed > 0) logger.info("Index[{}]: sort keys written into {} stored documents", indexName, keyed)
        return keyed
    }

    /** Keys the document under [key] if it is still the one read; retried on a document changed meanwhile. */
    suspend fun key(key: String): Outcome {
        var outcome: Outcome? = null
        var attempts = 0
        while (outcome == null && attempts < ATTEMPTS) {
            attempts++
            outcome = attempt(key)
        }
        if (outcome == null) {
            logger.warn("Index[{}]: stored document [{}] changed {} times while keyed", indexName, key, ATTEMPTS)
        }
        return outcome ?: Outcome.UNKEYABLE
    }

    // Null when the document changed between its read and the write.
    private suspend fun attempt(key: String): Outcome? {
        val read = readRaw(key)
        val document = read?.let { parse(key, it) }
        val expected = document?.let { sortKeys.keysOf(document = it, id = idOf(key)) }
        return when {
            read == null -> Outcome.GONE
            document == null || expected == null -> Outcome.UNKEYABLE
            document.get(SortKeys.DOCUMENT_FIELD) == expected -> Outcome.CURRENT
            writeIfUnchanged(key = key, read = read, sortKeys = expected.toString()) -> Outcome.KEYED
            else -> null
        }
    }

    /** Writes [sortKeys] into the document under [key] only if it still reads [read]; whether it did. */
    suspend fun writeIfUnchanged(key: String, read: String, sortKeys: String): Boolean =
        commands.eval<Long>(WRITE_IF_UNCHANGED, ScriptOutputType.INTEGER, arrayOf(key), read, sortKeys)
            .awaitFirst() == 1L

    private fun isCurrent(key: String, json: String?): Boolean {
        val document = json?.let { parseOrNull(it) } ?: return false
        return document.get(SortKeys.DOCUMENT_FIELD) == sortKeys.keysOf(document = document, id = idOf(key))
    }

    private fun parse(key: String, json: String): ObjectNode? = parseOrNull(json).also { document ->
        if (document == null) {
            logger.warn("Index[{}]: stored document [{}] is not a JSON object; left without sort keys", indexName, key)
        }
    }

    private fun parseOrNull(json: String): ObjectNode? =
        try {
            objectMapper.readTree(json) as? ObjectNode
        } catch (_: JacksonException) {
            null
        }

    private fun idOf(key: String) = key.removePrefix("$indexName-")

    // The script reads the document with the same command, so the answer compared is byte for byte the one read.
    private suspend fun readRaw(key: String): String? =
        commands.dispatch<String>(JSON_GET, ValueOutput(StringCodec.UTF8), CommandArgs(StringCodec.UTF8).addKey(key))
            .awaitFirstOrNull()

    private companion object {
        const val ATTEMPTS = 5

        val JSON_GET = SearchCommand.cmd("JSON.GET")

        val WRITE_IF_UNCHANGED = """
            if redis.call('JSON.GET', KEYS[1]) == ARGV[1] then
              redis.call('JSON.SET', KEYS[1], '$.${SortKeys.DOCUMENT_FIELD}', ARGV[2])
              return 1
            end
            return 0
        """.trimIndent()

        val logger: Logger = LoggerFactory.getLogger(RedisSnapView::class.java)
    }
}
