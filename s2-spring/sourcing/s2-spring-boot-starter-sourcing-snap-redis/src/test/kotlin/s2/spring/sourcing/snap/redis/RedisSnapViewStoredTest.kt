package s2.spring.sourcing.snap.redis

import io.lettuce.core.ScanArgs
import io.lettuce.core.ScanIterator
import io.lettuce.core.json.JsonPath
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Reads by key, past what the search index answers, and the batch reads' strictness. */
class RedisSnapViewStoredTest {

    data class WindowProbeSnap(val id: String)

    private companion object {
        const val BEYOND_WINDOW = SEARCH_WINDOW + 1
        const val SCAN_BATCH = 1_000L
    }

    private val view = RedisStack.view(
        WindowProbeSnap::class,
        listOf(RedisIndexField(WindowProbeSnap::id.name, RedisFieldType.TAG)),
        WindowProbeSnap::id,
    )
    private val sync = RedisStack.connection.sync()

    @BeforeEach
    fun clean() = dropAll()

    @AfterEach
    fun tidy() = dropAll()

    private fun storeRaw(ids: List<String>) {
        val async = RedisStack.connection.async()
        ids.map { async.jsonSet(view.key(it), JsonPath.ROOT_PATH, """{"id":"$it"}""") }
            .forEach { it.get(1, TimeUnit.MINUTES) }
    }

    @Test
    suspend fun `every snapshot of the model is deleted, readable or not, and no other model's`() {
        sync.jsonSet("WindowProbeSnapshot-x", JsonPath.ROOT_PATH, """{"id":"x"}""")
        sync.jsonSet(view.key("readable"), JsonPath.ROOT_PATH, """{"id":"readable"}""")
        sync.jsonSet(view.key("former"), JsonPath.ROOT_PATH, """{"formerId":"x"}""")

        assertThat(view.deleteAll()).isEqualTo(2)
        assertThat(sync.exists("WindowProbeSnap-readable", "WindowProbeSnap-former")).isZero()
        assertThat(sync.exists("WindowProbeSnapshot-x")).isEqualTo(1)
    }

    @Test
    suspend fun `more snapshots than one batch holds are deleted`() {
        storeRaw((1..STORE_BATCH * 2 + 1).map { "d$it" })

        assertThat(view.deleteAll()).isEqualTo(STORE_BATCH * 2 + 1)
        assertThat(view.isEmpty()).isTrue()
    }

    @Test
    suspend fun `every snapshot is read past the search window, each once`() {
        storeRaw((1..BEYOND_WINDOW).map { "w$it" })

        val ids = view.stored().map { it.id }.toList()

        assertThat(ids).hasSize(BEYOND_WINDOW).doesNotHaveDuplicates().contains("w1", "w$BEYOND_WINDOW")
    }

    @Test
    suspend fun `the store is read as the flow is collected, batch by batch`() {
        storeRaw((1..STORE_BATCH * 3).map { "s$it" })

        assertThat(view.stored().take(2).toList()).hasSize(2)
    }

    @Test
    suspend fun `a snapshot of another model sharing the name's first letters is not read`() {
        sync.jsonSet("WindowProbeSnapshot-x", JsonPath.ROOT_PATH, """{"id":"x"}""")
        sync.jsonSet(view.key("own"), JsonPath.ROOT_PATH, """{"id":"own"}""")

        assertThat(view.stored().toList().map { it.id }).containsExactly("own")
        assertThat(view.isEmpty()).isFalse()
    }

    @Test
    fun `a snapshot the reader cannot read stops every read and names its key`() {
        sync.jsonSet(view.key("former"), JsonPath.ROOT_PATH, """{"formerId":"x"}""")
        val reads = listOf<suspend () -> Any?>(
            { view.stored().toList() },
            { view.getAll(listOf("former")) },
            { view.get("former") },
        )

        reads.forEach { read ->
            assertThat(runCatching { runBlocking { read() } }.exceptionOrNull())
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("Snapshot [WindowProbeSnap-former] is not a readable WindowProbeSnap")
        }
    }

    @Test
    suspend fun `a model with nothing stored reads empty`() {
        assertThat(view.stored().toList()).isEmpty()
        assertThat(view.isEmpty()).isTrue()
    }

    @Test
    suspend fun `many ids are read in one batch, an unknown or repeated id answered once or not at all`() {
        view.save(WindowProbeSnap("a"))
        view.save(WindowProbeSnap("b"))

        val read = view.getAll(listOf("a", "b", "a", "unknown"))

        assertThat(read).containsExactlyInAnyOrderEntriesOf(mapOf("a" to WindowProbeSnap("a"), "b" to WindowProbeSnap("b")))
        assertThat(view.getAll(emptyList())).isEmpty()
    }

    @Test
    suspend fun `a snapshot is written, read back and removed by its id`() {
        val saved = view.save(WindowProbeSnap("one"))

        assertThat(saved).isEqualTo(WindowProbeSnap("one"))
        assertThat(view.get("one")).isEqualTo(WindowProbeSnap("one"))
        assertThat(sync.jsonGet("WindowProbeSnap-one", JsonPath.of("$.type")).toString()).contains("WindowProbeSnap")
        assertThat(view.remove("one")).isTrue()
        assertThat(view.remove("one")).isFalse()
        assertThat(view.get("one")).isNull()
    }

    @Test
    suspend fun `an index reads its own model's keys only, not a model whose name starts the same`() {
        sync.jsonSet("WindowProbeSnapshot-x", JsonPath.ROOT_PATH, """{"id":"x"}""")
        view.save(WindowProbeSnap("own"))

        view.initIndex()

        assertThat(view.count()).isEqualTo(1)
        assertThat(view.search(null, null, null).items).containsExactly(WindowProbeSnap("own"))
    }

    @Test
    suspend fun `more ids than one batch holds are all read`() {
        val ids = (1..STORE_BATCH + 1).map { "b$it" }
        storeRaw(ids)

        assertThat(view.getAll(ids)).hasSize(STORE_BATCH + 1)
    }

    private fun dropAll() = runBlocking {
        view.dropIndex()
        listOf("WindowProbeSnap-*", "WindowProbeSnapshot-*").forEach { pattern ->
            val keys = mutableListOf<String>()
            ScanIterator.scan(sync, ScanArgs.Builder.matches(pattern).limit(SCAN_BATCH)).forEach { keys += it }
            keys.chunked(SCAN_BATCH.toInt()).forEach { sync.unlink(*it.toTypedArray()) }
        }
    }
}
