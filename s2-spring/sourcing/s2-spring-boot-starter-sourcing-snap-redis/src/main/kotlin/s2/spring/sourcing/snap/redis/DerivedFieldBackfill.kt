package s2.spring.sourcing.snap.redis

import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.reactive.RedisReactiveCommands
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.output.IntegerOutput
import io.lettuce.core.output.ValueOutput
import io.lettuce.core.protocol.CommandArgs
import kotlin.time.TimeSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tools.jackson.core.JacksonException
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ObjectNode

/**
 * Gives every stored document of a view the fields it derives from it — sort keys, folded texts — without decoding a
 * snapshot or replaying an event: a document saved before they existed, or before an attribute was added, would sort
 * as a missing text and not be found by its text. They are written only if the document is still the one they were
 * computed from, so a `save` running meanwhile is never undone.
 */
internal class DerivedFieldBackfill(
    private val connection: StatefulRedisConnection<String, String>,
    private val indexName: String,
    private val keys: SnapshotKeys<*>,
    derived: List<DerivedField>,
    private val objectMapper: ObjectMapper,
) {
    private val derived = derived.filterNot { it.isEmpty }

    private val commands: RedisReactiveCommands<String, String>
        get() = connection.commands(RedisReactiveCommands.factory())

    enum class Outcome { CURRENT, WRITTEN, GONE, UNWRITABLE }

    /**
     * Writes the derived fields of every stored document lacking them, or holding stale ones; how many it wrote. A
     * key holding no JSON document is left alone, as every read of the view leaves it.
     */
    suspend fun run(): Int {
        if (derived.isEmpty()) return 0
        val started = TimeSource.Monotonic.markNow()
        val counts = IntArray(Outcome.entries.size)
        val seen = HashSet<String>()
        keys.batches().collect { batch ->
            val fresh = batch.filter(seen::add)
            val stale = ArrayList<String>()
            fresh.zip(keys.read(fresh)).forEach { (key, value) ->
                val document = value.rootDocument()
                when {
                    document == null -> counts[Outcome.GONE.ordinal]++
                    isCurrent(key, document) -> counts[Outcome.CURRENT.ordinal]++
                    else -> stale += key
                }
            }
            // One batch in flight at once: the connection pipelines its reads and writes.
            coroutineScope { stale.map { key -> async { write(key) } }.awaitAll() }.forEach { counts[it.ordinal]++ }
        }
        logger.info(
            "Index[{}]: {} of {} stored documents read in {}: {} written, {} could not be",
            indexName,
            derived.map { it.documentField },
            seen.size,
            started.elapsedNow(),
            counts[Outcome.WRITTEN.ordinal],
            counts[Outcome.UNWRITABLE.ordinal],
        )
        return counts[Outcome.WRITTEN.ordinal]
    }

    /** Writes the derived fields of the document under [key] if it is still the one read; retried if it changed. */
    suspend fun write(key: String): Outcome {
        var outcome: Outcome? = null
        var attempts = 0
        while (outcome == null && attempts < ATTEMPTS) {
            attempts++
            outcome = attempt(key)
        }
        if (outcome == null) {
            logger.warn("Index[{}]: stored document [{}] changed {} times while written", indexName, key, ATTEMPTS)
        }
        return outcome ?: Outcome.UNWRITABLE
    }

    // Null when the document changed between its read and the write.
    private suspend fun attempt(key: String): Outcome? {
        val read = readRaw(key)
        val document = read?.let { parse(key, it) }
        val stale = document?.let { staleOf(key = key, document = it) }
        return when {
            read == null -> Outcome.GONE
            stale == null -> Outcome.UNWRITABLE
            stale.isEmpty() -> Outcome.CURRENT
            writeIfUnchanged(key = key, read = read, values = stale) -> Outcome.WRITTEN
            else -> null
        }
    }

    /**
     * Writes each of [values], a JSON value by document field, into the document under [key] only if it still reads
     * [read]; whether it did.
     */
    suspend fun writeIfUnchanged(key: String, read: String, values: Map<String, String>): Boolean {
        val args = CommandArgs(StringCodec.UTF8).add(WRITE_IF_UNCHANGED).add(1).addKey(key).addValue(read)
        values.forEach { (field, value) -> args.addValue("$.$field").addValue(value) }
        return commands.dispatch<Long>(EVAL, IntegerOutput(StringCodec.UTF8), args).awaitFirst() == 1L
    }

    private fun isCurrent(key: String, json: String): Boolean =
        parseOrNull(json)?.let { staleOf(key = key, document = it).isEmpty() } == true

    private fun staleOf(key: String, document: ObjectNode): Map<String, String> =
        derived.associate { it.documentField to it.valueOf(document = document, id = idOf(key)) }
            .filter { (field, value) -> document.get(field) != value }
            .mapValues { (_, value) -> value.toString() }

    private fun parse(key: String, json: String): ObjectNode? = parseOrNull(json).also { document ->
        if (document == null) {
            logger.warn(
                "Index[{}]: stored document [{}] is not a JSON object; left without derived fields",
                indexName,
                key,
            )
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

        val EVAL = SearchCommand.cmd("EVAL")

        val WRITE_IF_UNCHANGED = """
            if redis.call('JSON.GET', KEYS[1]) == ARGV[1] then
              for i = 2, #ARGV, 2 do
                redis.call('JSON.SET', KEYS[1], ARGV[i], ARGV[i + 1])
              end
              return 1
            end
            return 0
        """.trimIndent()

        val logger: Logger = LoggerFactory.getLogger(RedisSnapView::class.java)
    }
}
