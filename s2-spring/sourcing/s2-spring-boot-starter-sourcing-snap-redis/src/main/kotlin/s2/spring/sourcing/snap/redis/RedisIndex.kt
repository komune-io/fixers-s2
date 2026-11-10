package s2.spring.sourcing.snap.redis

import io.lettuce.core.RedisCommandExecutionException
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.reactive.RedisReactiveCommands
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.output.ComplexDataParser
import io.lettuce.core.output.ComplexOutput
import io.lettuce.core.output.StatusOutput
import io.lettuce.core.protocol.CommandArgs
import io.lettuce.core.protocol.ProtocolKeyword
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory

/**
 * The RediSearch index of one view: made to match its [definition], whose fingerprint is written under
 * `<index>:definition` — outside every view's key prefix — since `FT.INFO` does not show all of it.
 */
internal class RedisIndex(
    private val connection: StatefulRedisConnection<String, String>,
    val name: String,
    private val definition: IndexDefinition,
) {
    private val definitionKey = "$name:definition"

    private val commands: RedisReactiveCommands<String, String>
        get() = connection.commands(RedisReactiveCommands.factory())

    /**
     * Makes the index match its definition — created when absent, kept when the definition written next to it is
     * this one, dropped and re-created otherwise, its documents kept — then waits until it has read them. An index
     * kept with documents it could not read is made again once; any failure left fails.
     */
    suspend fun init() {
        val isMadeNow = make()
        val failures = awaitIndexed().failures()
        if (failures == 0L) return
        check(!isMadeNow) { "Index[$name]: ${unindexed(failures)}" }
        logger.info("Index[{}] made again: {}", name, unindexed(failures))
        remake()
        val left = awaitIndexed().failures()
        check(left == 0L) { "Index[$name]: ${unindexed(left)}" }
    }

    private fun unindexed(count: Long) = "$count stored documents could not be indexed"

    private suspend fun make(): Boolean {
        val current = info()
        val written = commands[definitionKey].awaitSingleOrNull()
        return when {
            current == null -> true.also { create() }
            written == definition.fingerprint -> false.also { logger.debug("Index[{}] already exists", name) }
            else -> true.also {
                logger.info("Index[{}] made again: its definition differs from [{}]", name, written)
                remake()
            }
        }
    }

    private suspend fun remake() {
        try {
            dispatch(SearchCommand.FT_DROPINDEX, CommandArgs(StringCodec.UTF8).add(name))
        } catch (e: RedisCommandExecutionException) {
            if (!e.isIndexAbsent()) throw e
        }
        create()
    }

    private suspend fun create() {
        try {
            dispatch(SearchCommand.FT_CREATE, definition.createArgs(name))
        } catch (e: RedisCommandExecutionException) {
            // Another instance of the same view, starting at the same time, created it first.
            if (!e.isIndexAlreadyPresent()) throw e
            logger.debug("Index[{}] already exists", name, e)
        }
        check(commands.set(definitionKey, definition.fingerprint).awaitSingle() == "OK") {
            "Index[$name]: its definition could not be written to $definitionKey"
        }
    }

    /** Waits until the index has read the stored documents, at most [ceiling]; answers its last `FT.INFO`. */
    suspend fun awaitIndexed(ceiling: Duration = INDEXING_CEILING): Map<String, Any?> =
        withTimeoutOrNull(ceiling) {
            var info = checkNotNull(info()) { "Index[$name] absent once made" }
            while (!info.hasReadStoredDocuments()) {
                delay(INDEXING_POLL)
                info = checkNotNull(info()) { "Index[$name] dropped while it was indexing" }
            }
            info
        } ?: error("Index[$name] still indexing after $ceiling")

    /** `FT.INFO` of [index], as plain maps, lists and scalars; null when there is no such index. */
    suspend fun info(index: String = name): Map<String, Any?>? =
        try {
            val output = ComplexOutput<String, String, Any?>(StringCodec.UTF8, ComplexDataParser<Any?> { plain(it) })
            val reply = commands
                .dispatch<Any?>(SearchCommand.FT_INFO, output, CommandArgs<String, String>(StringCodec.UTF8).add(index))
                .awaitFirstOrNull()
            asFields(reply)
        } catch (e: RedisCommandExecutionException) {
            if (e.isIndexAbsent()) null else throw e
        }

    /** A module command, sent like every other: a blocking call could wait on the thread that reads its answer. */
    suspend fun dispatch(command: ProtocolKeyword, args: CommandArgs<String, String>) {
        commands.dispatch<String>(command, StatusOutput(StringCodec.UTF8), args).awaitFirstOrNull()
    }

    companion object {
        /** How long a view waits, at most, for its index to read the stored documents. */
        val INDEXING_CEILING: Duration = 60.seconds

        private val INDEXING_POLL = 100.milliseconds

        private val logger = LoggerFactory.getLogger(RedisSnapView::class.java)
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

// RESP3 answers `indexing` as 0.0, RESP2 as "0".
private fun Map<String, Any?>.hasReadStoredDocuments(): Boolean =
    get("indexing")?.run { toString().toDoubleOrNull() } == 0.0

/** The stored documents `FT.INFO` says the index could not read. */
internal fun Map<String, Any?>.failures(): Long = get("hash_indexing_failures")?.toString()?.toDoubleOrNull()?.toLong()
    ?: error("FT.INFO carries no hash_indexing_failures: $this")

internal fun Throwable.isIndexAlreadyPresent(): Boolean =
    this is RedisCommandExecutionException && message.orEmpty().contains("Index already exists", ignoreCase = true)

internal fun Throwable.isIndexAbsent(): Boolean =
    this is RedisCommandExecutionException && message.orEmpty().let {
        it.contains("Unknown index", ignoreCase = true) || it.contains("no such index", ignoreCase = true)
    }
