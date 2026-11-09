package s2.spring.sourcing.snap.redis

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisCommandExecutionException
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.output.StatusOutput
import java.util.UUID
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

/** Index creation tells an index already there from one that could not be made, and waits for it to read. */
class RedisSnapViewIndexTest {

	data class IndexProbeSnap(val id: String)

	private companion object {
		const val ALIAS = "IndexProbeSnapAlias"
		const val STORED = 5_000
		const val CANCEL_AFTER_MS = 300L
		const val ABSENT = "IndexProbeSnapAbsent"
		const val SHORT_DEADLINE_NS = 1_500_000_000L
		val ID = RedisIndexField(IndexProbeSnap::id.name, RedisFieldType.TAG)
	}

	private val view = RedisStack.view()
	private val logged = ListAppender<ILoggingEvent>()
	private val loggers = listOf(RedisSnapView::class.java, IndexingStart::class.java)
		.map { LoggerFactory.getLogger(it) as Logger }

	@BeforeEach
	suspend fun prepare() {
		view.dropIndex<IndexProbeSnap>()
		logged.start()
		loggers.forEach {
			it.level = Level.DEBUG
			it.addAppender(logged)
		}
	}

	@AfterEach
	suspend fun tidy() {
		loggers.forEach { it.detachAppender(logged) }
		view.dropIndex<IndexProbeSnap>()
		view.deleteAll<IndexProbeSnap>()
	}

	private fun levelsOf(index: String) =
		logged.list.filter { it.formattedMessage.startsWith("Index[$index]") }.map { it.level }

	private suspend fun storeMany() = (1..STORED).map { "index-probe-${UUID.randomUUID()}" }
		.forEach { view.save(it, IndexProbeSnap(it)) }

	@Test
	suspend fun `an index already there is a debug line`() {
		view.createIndex<IndexProbeSnap>(ID)
		view.createIndex<IndexProbeSnap>(ID)

		assertThat(levelsOf("IndexProbeSnap")).containsExactly(Level.DEBUG)
	}

	@Test
	suspend fun `an index the store refuses to make is an error`() {
		view.createIndex<IndexProbeSnap>(ID, ID)

		assertThat(levelsOf("IndexProbeSnap")).containsExactly(Level.ERROR)
	}

	@Test
	suspend fun `an index whose definition matches is kept, not dropped`() {
		view.createIndex<IndexProbeSnap>(ID)
		val sync = RedisStack.connection.sync()
		// An alias dies with its index: still answering, the index was never dropped.
		sync.ftAliasadd(ALIAS, "IndexProbeSnap")
		try {
			view.createIndex<IndexProbeSnap>(ID)

			assertThat(view.indexInfo(ALIAS)).isNotNull()
		} finally {
			sync.ftAliasdel(ALIAS)
		}
	}

	@Test
	suspend fun `an index of an older definition is replaced, stopwords included`() {
		val older = IndexDefinition.of(listOf(ID), RedisStack.settings).copy(keepsStopwords = true)
		view.make("IndexProbeSnap", older)
		assertThat(definitionOf("IndexProbeSnap").keepsStopwords).isTrue()

		view.createIndex<IndexProbeSnap>(ID, RedisIndexField("name", RedisFieldType.TEXT))

		val read = definitionOf("IndexProbeSnap")
		assertThat(read.keepsStopwords).isFalse()
		assertThat(logged.list.map { it.formattedMessage }).anyMatch { it.contains("made again (definition differs)") }
		assertThat(read.attributes.map { it.name }).containsExactlyInAnyOrder("id", "name")
		assertThat(read.attributes.first { it.name == "name" }.flags).containsExactlyInAnyOrder("SORTABLE", "NOSTEM")
	}

	@Test
	suspend fun `an index made in another language is made again in the view's own`() {
		RedisStack.view(settings = IndexSettings(IndexLanguage.ENGLISH, null)).createIndex<IndexProbeSnap>(ID)
		assertThat(definitionOf("IndexProbeSnap").language).isEqualTo("english")

		view.createIndex<IndexProbeSnap>(ID)

		assertThat(definitionOf("IndexProbeSnap").language).isEqualTo(RedisStack.settings.language.value)
		assertThat(logged.list.map { it.formattedMessage }).anyMatch { it.contains("made again (definition differs)") }
	}

	@Test
	suspend fun `an index in RediSearch's default language is kept, though FT INFO does not name it`() {
		val english = RedisStack.view(settings = IndexSettings(IndexLanguage.ENGLISH, null))
		english.createIndex<IndexProbeSnap>(ID)
		logged.list.clear()

		english.createIndex<IndexProbeSnap>(ID)

		assertThat(levelsOf("IndexProbeSnap")).containsExactly(Level.DEBUG)
	}

	@Test
	suspend fun `a re-created index has read the stored documents when creation returns`() {
		storeMany()

		view.createIndex<IndexProbeSnap>(ID)

		assertThat(view.count<IndexProbeSnap>()).isEqualTo(STORED.toLong())
	}

	@Test
	suspend fun `an index kept as it is has also read the stored documents when creation returns`() {
		storeMany()
		// Created behind the view's back, so the view finds it equal and still reading.
		RedisStack.connection.sync().dispatch(
			SearchCommand.FT_CREATE,
			StatusOutput(StringCodec.UTF8),
			IndexDefinition.of(listOf(ID), RedisStack.settings).createArgs("IndexProbeSnap"),
		)

		view.createIndex<IndexProbeSnap>(ID)

		assertThat(view.count<IndexProbeSnap>()).isEqualTo(STORED.toLong())
	}

	@Test
	suspend fun `a start makes every index first, then waits for them all`() {
		storeMany()
		val start = IndexingStart()

		withContext(start) { view.createIndex<IndexProbeSnap>(ID) }
		assertThat(start.pending).containsExactly("IndexProbeSnap")
		start.awaitAll()

		assertThat(start.pending).isEmpty()
		assertThat(view.count<IndexProbeSnap>()).isEqualTo(STORED.toLong())
	}

	@Test
	suspend fun `a wait past the start's deadline is a warning`() {
		IndexingStart(deadline = System.nanoTime()).apply { made(view, ABSENT) }.awaitAll()

		assertThat(levelsOf(ABSENT)).containsExactly(Level.WARN)
	}

	@Test
	suspend fun `an index done while an earlier one still reads is not warned`() {
		storeMany()
		val start = IndexingStart(deadline = System.nanoTime() + SHORT_DEADLINE_NS)
		// First in line, and never done: an index that does not exist.
		start.made(view, ABSENT)

		withContext(start) { view.createIndex<IndexProbeSnap>(ID) }
		start.awaitAll()

		assertThat(levelsOf(ABSENT)).containsExactly(Level.WARN)
		assertThat(levelsOf("IndexProbeSnap")).doesNotContain(Level.WARN)
	}

	/** An index that does not exist never finishes indexing: the wait can only end by its cancellation. */
	@Test
	suspend fun `a start cancelled while it waits is not logged as a failure`() {
		val finished = withTimeoutOrNull(CANCEL_AFTER_MS) {
			IndexingStart().apply { made(view, ABSENT) }.awaitAll()
		}

		assertThat(finished).isNull()
		assertThat(levelsOf(ABSENT)).isEmpty()
	}

	@Test
	suspend fun `a store that cannot answer is an error line, never a failed start`() {
		val closed = RedisClient.create(RedisStack.uri)
		try {
			val lost = closed.connect().also { it.close() }

			RedisStack.view(connection = lost).createIndex<IndexProbeSnap>(ID)

			assertThat(levelsOf("IndexProbeSnap")).containsExactly(Level.ERROR)
		} finally {
			closed.shutdown()
		}
	}

	private suspend fun definitionOf(index: String) =
		requireNotNull(IndexDefinition.read(requireNotNull(view.indexInfo(index))))

	@Test
	fun `only the store's own « unknown index » answer counts as nothing to drop`() {
		assertThat(RedisCommandExecutionException("Unknown Index name").isIndexAbsent()).isTrue()
		assertThat(RedisCommandExecutionException("Unknown index name").isIndexAbsent()).isTrue()
		assertThat(RedisCommandExecutionException("ERR wrong number of arguments").isIndexAbsent()).isFalse()
		assertThat(IllegalStateException("Unknown Index name").isIndexAbsent()).isFalse()
	}

	@Test
	fun `only the store's own « already exists » answer counts as an index already there`() {
		assertThat(RedisCommandExecutionException("Index already exists").isIndexAlreadyPresent()).isTrue()
		assertThat(RedisCommandExecutionException("Duplicate field in schema - id").isIndexAlreadyPresent()).isFalse()
		assertThat(IllegalStateException("Index already exists").isIndexAlreadyPresent()).isFalse()
	}
}
