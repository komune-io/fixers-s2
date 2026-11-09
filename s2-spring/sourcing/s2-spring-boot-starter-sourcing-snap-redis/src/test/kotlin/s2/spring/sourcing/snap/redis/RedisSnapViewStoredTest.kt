package s2.spring.sourcing.snap.redis

import io.lettuce.core.ScanArgs
import io.lettuce.core.ScanIterator
import io.lettuce.core.json.JsonPath
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Reads by key, past what the search index holds or answers, and the batch reads' strictness. */
class RedisSnapViewStoredTest {

	data class UnindexedProbeSnap(val id: String, val weight: Any?)
	data class WindowProbeSnap(val id: String)

	private companion object {
		const val BEYOND_WINDOW = SEARCH_WINDOW + 1
		const val SCAN_BATCH = 1_000L
	}

	private val view = RedisStack.view()
	private val sync = RedisStack.connection.sync()

	@BeforeEach
	fun clean() = dropAll()

	@AfterEach
	fun tidy() = dropAll()

	@Test
	suspend fun `a snapshot the search index dropped is still read from the store`() {
		view.createIndex<UnindexedProbeSnap>(
			RedisIndexField(UnindexedProbeSnap::id.name, RedisFieldType.TAG),
			RedisIndexField(UnindexedProbeSnap::weight.name, RedisFieldType.NUMERIC),
		)
		view.save("light", UnindexedProbeSnap(id = "light", weight = 1))
		view.save("heavy", UnindexedProbeSnap(id = "heavy", weight = 2))
		view.save("unreadable", UnindexedProbeSnap(id = "unreadable", weight = "not a number"))

		assertThat(view.count<UnindexedProbeSnap>())
			.describedAs("test precondition: RediSearch drops a document whose numeric field holds text")
			.isEqualTo(2)
		assertThat(view.stored<UnindexedProbeSnap>().map { it.id })
			.containsExactlyInAnyOrder("light", "heavy", "unreadable")
		assertThat(view.all<UnindexedProbeSnap>().toList().map { it.id })
			.containsExactlyInAnyOrder("light", "heavy", "unreadable")
	}

	@Test
	suspend fun `every snapshot of the model is deleted, readable or not, and no other model's`() {
		view.createIndex<WindowProbeSnap>(RedisIndexField(WindowProbeSnap::id.name, RedisFieldType.TAG))
		sync.jsonSet("WindowProbeSnapshot-x", JsonPath.ROOT_PATH, """{"id":"x"}""")
		sync.jsonSet(view.buildId<WindowProbeSnap>("readable"), JsonPath.ROOT_PATH, """{"id":"readable"}""")
		sync.jsonSet(view.buildId<WindowProbeSnap>("former"), JsonPath.ROOT_PATH, """{"formerId":"x"}""")

		assertThat(view.deleteAll<WindowProbeSnap>()).isEqualTo(2)
		assertThat(sync.exists("WindowProbeSnap-readable", "WindowProbeSnap-former")).isZero()
		assertThat(sync.exists("WindowProbeSnapshot-x")).isEqualTo(1)
	}

	@Test
	suspend fun `every snapshot is read past the search window`() {
		view.createIndex<WindowProbeSnap>(RedisIndexField(WindowProbeSnap::id.name, RedisFieldType.TAG))
		val async = RedisStack.connection.async()
		(1..BEYOND_WINDOW).map { n ->
			async.jsonSet(view.buildId<WindowProbeSnap>("w$n"), JsonPath.ROOT_PATH, """{"id":"w$n","type":"WindowProbeSnap"}""")
		}.forEach { it.get(1, TimeUnit.MINUTES) }

		val ids = view.stored<WindowProbeSnap>().map { it.id }

		assertThat(ids).hasSize(BEYOND_WINDOW).doesNotHaveDuplicates().contains("w1", "w$BEYOND_WINDOW")
	}

	@Test
	suspend fun `a snapshot of another model sharing the name's first letters is not read`() {
		sync.jsonSet("WindowProbeSnapshot-x", JsonPath.ROOT_PATH, """{"id":"x"}""")
		sync.jsonSet(view.buildId<WindowProbeSnap>("own"), JsonPath.ROOT_PATH, """{"id":"own"}""")

		assertThat(view.stored<WindowProbeSnap>().map { it.id }).containsExactly("own")
	}

	@Test
	fun `a snapshot the reader cannot read stops the read and names its key`() {
		sync.jsonSet(view.buildId<WindowProbeSnap>("readable"), JsonPath.ROOT_PATH, """{"id":"readable"}""")
		sync.jsonSet(view.buildId<WindowProbeSnap>("former"), JsonPath.ROOT_PATH, """{"formerId":"x"}""")

		assertThatThrownBy { runBlocking { view.stored<WindowProbeSnap>() } }
			.isInstanceOf(IllegalStateException::class.java)
			.hasMessageContaining("[WindowProbeSnap-former]")
	}

	@Test
	suspend fun `a model with nothing stored reads empty`() {
		assertThat(view.stored<WindowProbeSnap>()).isEmpty()
	}

	@Test
	suspend fun `many ids are read in one batch, an unknown or repeated id answered once or not at all`() {
		view.save("a", WindowProbeSnap("a"))
		view.save("b", WindowProbeSnap("b"))

		val read = view.getAll<WindowProbeSnap>(listOf("a", "b", "a", "unknown"))

		assertThat(read).containsOnlyKeys("a", "b")
		assertThat(view.getAll<WindowProbeSnap>(emptyList())).isEmpty()
	}

	@Test
	suspend fun `a snapshot is written, read back and deleted by its id`() {
		val saved = view.save("one", WindowProbeSnap("one"))

		assertThat(saved).isEqualTo(WindowProbeSnap("one"))
		assertThat(view.get<WindowProbeSnap>("one")).isEqualTo(WindowProbeSnap("one"))
		assertThat(sync.jsonGet("WindowProbeSnap-one", JsonPath.of("$.type")).toString()).contains("WindowProbeSnap")
		assertThat(view.delete<WindowProbeSnap>("one")).isEqualTo("one")
		assertThat(view.get<WindowProbeSnap>("one")).isNull()
	}

	private fun dropAll() = runBlocking {
		view.dropIndex<UnindexedProbeSnap>()
		view.dropIndex<WindowProbeSnap>()
		listOf("UnindexedProbeSnap-*", "WindowProbeSnap-*", "WindowProbeSnapshot-*").forEach { pattern ->
			val keys = mutableListOf<String>()
			ScanIterator.scan(sync, ScanArgs.Builder.matches(pattern).limit(SCAN_BATCH)).forEach { keys += it }
			keys.chunked(SCAN_BATCH.toInt()).forEach { sync.unlink(*it.toTypedArray()) }
		}
	}
}
