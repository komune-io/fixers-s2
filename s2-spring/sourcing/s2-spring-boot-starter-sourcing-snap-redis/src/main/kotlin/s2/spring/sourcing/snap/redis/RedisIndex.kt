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
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory

/**
 * The RediSearch index of one view: made to match its [definition], read back whole from `FT.INFO` — the definition's
 * marker attribute carries what `FT.INFO` would not show, so nothing is written beside the index.
 */
internal class RedisIndex(
    private val connection: StatefulRedisConnection<String, String>,
    val name: String,
    private val definition: IndexDefinition,
) {
    private val commands: RedisReactiveCommands<String, String>
        get() = connection.commands(RedisReactiveCommands.factory())

    /**
     * Makes the index match its definition — created when absent, kept when `FT.INFO` shows this one, dropped and
     * re-created otherwise, its documents kept — then waits until it has read them. An index
     * kept with documents it could not read is made again once; any failure left fails.
     */
    suspend fun init() {
        val isMadeNow = make()
        val failures = awaitIndexed().failures()
        if (failures == 0L) return
        check(!isMadeNow) { "Index[$name]: ${unindexed(failures)}" }
        logger.info("Index[{}] made again: {}", name, unindexed(failures))
        if (!dropAndCreate()) logger.info("Index[{}] made again by another writer", name)
        val left = awaitIndexed().failures()
        check(left == 0L) { "Index[$name]: ${unindexed(left)}" }
    }

    // `hash_indexing_failures` counts failed attempts, not documents: the initial scan may visit a key twice (Redis
    // SCAN repeats keys while the keyspace rehashes), and every rewrite of a refused document counts once more.
    private fun unindexed(failures: Long) = "stored documents could not be indexed (failed attempts: $failures)"

    // Kept only when FT.INFO shows this definition, marker included: another writer — an older version of the view,
    // a hand-made FT.CREATE, a replica starting at the same time — may have made the index, and is compared like any.
    private suspend fun make(): Boolean =
        (1..MAKE_ATTEMPTS).firstNotNullOfOrNull { attemptMake() }
            ?: error("Index[$name]: other writers made it $MAKE_ATTEMPTS times while this one compared it")

    // Whether the index is made now; null when another writer made it in between, to be compared again.
    private suspend fun attemptMake(): Boolean? {
        val current = info()
        return when {
            current == null -> {
                true.takeIf { tryCreate() }
            }
            definition.isShownBy(current) -> {
                logger.debug("Index[{}] already exists", name)
                false
            }
            else -> {
                logger.info("Index[{}] made again: FT.INFO shows another definition", name)
                true.takeIf { tryRemake() }
            }
        }
    }

    // Compared again right before the drop: a replica that made it meanwhile is not undone. False when another writer
    // made the index in between, which is then compared again.
    private suspend fun tryRemake(): Boolean = info()?.let(definition::isShownBy) != true && dropAndCreate()

    // Drops the index, its documents kept, and creates it again; false when another writer created it first.
    private suspend fun dropAndCreate(): Boolean {
        try {
            dispatch(SearchCommand.FT_DROPINDEX, CommandArgs(StringCodec.UTF8).add(name))
        } catch (e: RedisCommandExecutionException) {
            if (!e.isIndexAbsent()) throw e
        }
        return tryCreate()
    }

    // False when another writer created an index of this name in between: whatever its definition, FT.INFO shows it
    // whole, so the next comparison keeps it only if it is this one.
    private suspend fun tryCreate(): Boolean {
        try {
            dispatch(SearchCommand.FT_CREATE, definition.createArgs(name))
        } catch (e: RedisCommandExecutionException) {
            if (!e.isIndexExisting()) throw e
            return false
        }
        return true
    }

    /** Waits until the index has read the stored documents, at most [ceiling]; answers its last `FT.INFO`. */
    suspend fun awaitIndexed(ceiling: Duration = INDEXING_CEILING): Map<String, Any?> =
        withTimeoutOrNull(ceiling) {
            // Absent for a moment is no failure: a replica remaking the index drops it before it creates it again.
            var info = info()
            while (info == null || !info.hasReadStoredDocuments()) {
                delay(INDEXING_POLL)
                info = info()
            }
            info
        } ?: error("Index[$name] absent or still indexing after $ceiling")

    /**
     * `FT.INFO` of [index], as plain maps, lists and scalars; null when the store says there is no such index. An
     * answer that cannot be read as fields fails: it says nothing about whether the index exists.
     */
    suspend fun info(index: String = name): Map<String, Any?>? {
        val output = ComplexOutput<String, String, Any?>(StringCodec.UTF8, ComplexDataParser<Any?> { plain(it) })
        val reply = try {
            commands
                .dispatch<Any?>(SearchCommand.FT_INFO, output, CommandArgs<String, String>(StringCodec.UTF8).add(index))
                .awaitFirstOrNull()
        } catch (e: RedisCommandExecutionException) {
            if (e.isIndexAbsent()) return null
            throw e
        }
        return checkNotNull(asFields(reply)) { "Index[$index]: FT.INFO answer unreadable: ${reply ?: "none"}" }
    }

    /** A module command, sent like every other: a blocking call could wait on the thread that reads its answer. */
    suspend fun dispatch(command: ProtocolKeyword, args: CommandArgs<String, String>) {
        commands.dispatch<String>(command, StatusOutput(StringCodec.UTF8), args).awaitFirstOrNull()
    }

    companion object {
        /** How long a view waits, at most, for its index to read the stored documents. */
        val INDEXING_CEILING: Duration = 60.seconds

        private val INDEXING_POLL = 100.milliseconds

        private const val MAKE_ATTEMPTS = 3

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

/** The failed attempts to read a stored document, as `FT.INFO` counts them: one document may count several times. */
internal fun Map<String, Any?>.failures(): Long = get("hash_indexing_failures")?.toString()?.toDoubleOrNull()?.toLong()
    ?: error("FT.INFO carries no hash_indexing_failures: $this")

internal fun Throwable.isIndexExisting(): Boolean =
    this is RedisCommandExecutionException && message.orEmpty().contains("already exists", ignoreCase = true)

internal fun Throwable.isIndexAbsent(): Boolean =
    this is RedisCommandExecutionException && message.orEmpty().let {
        it.contains("Unknown index", ignoreCase = true) || it.contains("no such index", ignoreCase = true)
    }
