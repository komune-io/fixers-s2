package s2.spring.sourcing.snap.redis

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisCommandExecutionException
import io.lettuce.core.RedisException
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.output.StatusOutput
import io.lettuce.core.protocol.CommandArgs
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

/** An index is made to match its definition, kept when it does, and read whole before the start goes on. */
class RedisSnapViewIndexTest {

    data class IndexProbeSnap(val id: String, val name: String = "", val weight: Any? = 0)

    private val view = probeView(ID)
    private val sync = RedisStack.connection.sync()
    private val logged = ListAppender<ILoggingEvent>()
    private val logger = LoggerFactory.getLogger(RedisSnapView::class.java) as Logger

    private fun probeView(
        vararg fields: RedisIndexField,
        settings: IndexSettings = RedisStack.settings,
    ): RedisSnapView<IndexProbeSnap> =
        RedisStack.view(
            model = IndexProbeSnap::class,
            fields = fields.toList(),
            idOf = IndexProbeSnap::id,
            settings = settings,
        )

    @BeforeEach
    fun prepare() {
        runBlocking { tidy() }
        logged.start()
        logger.level = Level.DEBUG
        logger.addAppender(logged)
    }

    @AfterEach
    fun detach() {
        logger.detachAppender(logged)
        runBlocking { tidy() }
    }

    private suspend fun tidy() {
        view.dropIndex()
        val _ = view.deleteAll()
    }

    private fun messages() = logged.list.filter { it.formattedMessage.startsWith("Index[IndexProbeSnap]") }

    private suspend fun storeMany() {
        repeat(STORED) { val _ = view.save(IndexProbeSnap("index-probe-${UUID.randomUUID()}")) }
    }

    @Test
    suspend fun `an index whose definition matches is kept, not dropped`() {
        view.initIndex()
        // An alias dies with its index: still answering, the index was never dropped.
        sync.ftAliasadd(ALIAS, "IndexProbeSnap")
        try {
            logged.list.clear()

            view.initIndex()

            assertThat(view.index.info(ALIAS)).isNotNull()
            assertThat(messages().map { it.level }).containsExactly(Level.DEBUG)
        } finally {
            sync.ftAliasdel(ALIAS)
        }
    }

    @Test
    suspend fun `an index of another definition is made again, its documents kept`() {
        val _ = view.save(IndexProbeSnap("kept", name = "Saint-Égrève"))
        view.initIndex()

        val wider = probeView(ID, NAME)
        wider.initIndex()

        assertThat(messages().map { it.formattedMessage }).anyMatch { it.contains("definition differs") }
        assertThat(wider.search("Égrève".addWildcard(), null, null).items.map { it.id }).containsExactly("kept")
    }

    @Test
    suspend fun `a change of phonetic matcher makes the index again, though FT INFO does not show it`() {
        probeView(ID, NAME, settings = IndexSettings(null)).initIndex()
        logged.list.clear()

        probeView(ID, NAME, settings = IndexSettings(PhoneticMatcher.FRENCH)).initIndex()

        assertThat(messages().map { it.formattedMessage }).anyMatch { it.contains("definition differs") }
        assertThat(sync["IndexProbeSnap:definition"]).contains("PHONETIC dm:fr")
    }

    @Test
    suspend fun `an index another writer replaced is made again, though its written definition is this one`() {
        view.initIndex()
        view.index.dispatch(SearchCommand.FT_DROPINDEX, CommandArgs(StringCodec.UTF8).add("IndexProbeSnap"))
        sync.dispatch(
            SearchCommand.FT_CREATE,
            StatusOutput(StringCodec.UTF8),
            IndexDefinition.from("IndexProbeSnap", listOf(ID, NAME), RedisStack.settings).createArgs("IndexProbeSnap"),
        )
        logged.list.clear()

        view.initIndex()

        assertThat(messages().map { it.formattedMessage }).anyMatch { it.contains("definition differs") }
        assertThat(IndexDefinition.from("IndexProbeSnap", listOf(ID), RedisStack.settings).isShownBy(
            checkNotNull(view.index.info()),
        )).isTrue()
    }

    @Test
    suspend fun `an index made without its definition written next to it is made again`() {
        sync.dispatch(
            SearchCommand.FT_CREATE,
            StatusOutput(StringCodec.UTF8),
            IndexDefinition.from("IndexProbeSnap", listOf(ID), RedisStack.settings).createArgs("IndexProbeSnap"),
        )

        view.initIndex()

        assertThat(messages().map { it.formattedMessage }).anyMatch { it.contains("definition differs from [null]") }
    }

    @Test
    suspend fun `a re-created index has read the stored documents when creation returns`() {
        storeMany()

        view.initIndex()

        assertThat(view.count()).isEqualTo(STORED.toLong())
    }

    @Test
    suspend fun `an index kept as it is has also read the stored documents when creation returns`() {
        storeMany()
        // Created behind the view's back with its definition written, so the view keeps it while it still reads.
        val definition = IndexDefinition.from("IndexProbeSnap", listOf(ID), RedisStack.settings)
        sync["IndexProbeSnap:definition"] = definition.fingerprint
        sync.dispatch(SearchCommand.FT_CREATE, StatusOutput(StringCodec.UTF8), definition.createArgs("IndexProbeSnap"))

        view.initIndex()

        assertThat(messages().map { it.level }).containsExactly(Level.DEBUG)
        assertThat(view.count()).isEqualTo(STORED.toLong())
    }

    @Test
    suspend fun `a wait past its ceiling fails`() {
        storeMany()
        sync.dispatch(
            SearchCommand.FT_CREATE,
            StatusOutput(StringCodec.UTF8),
            IndexDefinition.from("IndexProbeSnap", listOf(ID), RedisStack.settings).createArgs("IndexProbeSnap"),
        )

        assertThatThrownBy { runBlocking { val _ = view.index.awaitIndexed(1.milliseconds) } }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Index[IndexProbeSnap] still indexing after 1ms")
    }

    @Test
    fun `a stored document the new index cannot read fails the start`() {
        val weighted = probeView(ID, WEIGHT)
        runBlocking { weighted.save(IndexProbeSnap("unreadable", weight = "not a number")) }

        assertThatThrownBy { runBlocking { weighted.initIndex() } }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Index[IndexProbeSnap]: 1 stored documents could not be indexed")
    }

    @Test
    suspend fun `a kept index that once failed a document is made again, and passes once the document is gone`() {
        val weighted = probeView(ID, WEIGHT)
        weighted.initIndex()
        val _ = weighted.save(IndexProbeSnap("unreadable", weight = "not a number"))
        assertThat(weighted.remove("unreadable")).isTrue()
        logged.list.clear()

        weighted.initIndex()

        // The store counts every failed attempt, not the documents left unindexed.
        assertThat(messages().map { it.formattedMessage })
            .anyMatch { it.startsWith("Index[IndexProbeSnap] made again: ") }
            .anyMatch { it.endsWith(" stored documents could not be indexed") }
    }

    @Test
    fun `a store that cannot answer fails the start`() {
        val closed = RedisClient.create(RedisStack.uri)
        try {
            val lost = closed.connect().apply { close() }

            assertThatThrownBy {
                runBlocking {
                    RedisStack.view(
                        model = IndexProbeSnap::class,
                        fields = listOf(ID),
                        idOf = IndexProbeSnap::id,
                        connection = lost,
                    ).initIndex()
                }
            }.isInstanceOf(RedisException::class.java)
        } finally {
            closed.shutdown()
        }
    }

    @Test
    fun `only the store's own « unknown index » answer counts as nothing to drop`() {
        assertThat(RedisCommandExecutionException("Unknown Index name").isIndexAbsent()).isTrue()
        assertThat(RedisCommandExecutionException("Unknown index name").isIndexAbsent()).isTrue()
        assertThat(RedisCommandExecutionException("ERR wrong number of arguments").isIndexAbsent()).isFalse()
        assertThat(IllegalStateException("Unknown Index name").isIndexAbsent()).isFalse()
    }


    private companion object {
        const val ALIAS = "IndexProbeSnapAlias"
        const val STORED = 5_000
        val ID = RedisIndexField(IndexProbeSnap::id.name, RedisFieldType.TAG)
        val NAME = RedisIndexField(IndexProbeSnap::name.name, RedisFieldType.TEXT)
        val WEIGHT = RedisIndexField(IndexProbeSnap::weight.name, RedisFieldType.NUMERIC)
    }
}
