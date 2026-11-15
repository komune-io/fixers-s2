package s2.spring.sourcing.snap.redis

import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** What a text, tag or numeric clause matches: the same answer on every supported Redis Stack. */
class RedisSnapViewMatchTest {

    data class MatchProbeSnap(val id: String, val name: String, val status: String = "", val rank: Int = 0)

    private val view = RedisStack.view(
        model = MatchProbeSnap::class,
        fields = listOf(
            RedisIndexField(MatchProbeSnap::id.name, RedisFieldType.TAG),
            RedisIndexField(MatchProbeSnap::name.name, RedisFieldType.TEXT),
            RedisIndexField(MatchProbeSnap::status.name, RedisFieldType.TAG),
            RedisIndexField(MatchProbeSnap::rank.name, RedisFieldType.NUMERIC),
        ),
        idOf = MatchProbeSnap::id,
    )

    @BeforeEach
    fun prepare() {
        runBlocking {
            val _ = view.deleteAll()
            view.initIndex()
        }
    }

    @AfterEach
    fun tidy() {
        runBlocking {
            val _ = view.deleteAll()
            view.dropIndex()
        }
    }

    private suspend fun store(vararg snaps: MatchProbeSnap): List<String> = snaps.map { snap ->
        "match-probe-${UUID.randomUUID()}".also { id -> val _ = view.save(snap.copy(id = id)) }
    }

    private suspend fun names(query: String) = view.search(query, null, null).items.map { it.name }.sorted()

    private fun snap(name: String, status: String = "", rank: Int = 0) =
        MatchProbeSnap(id = "", name = name, status = status, rank = rank)

    @Test
    suspend fun `a typed word finds its name whatever its case`() {
        val _ = store(snap("Charpentier"), snap("COFOR"))

        assertThat(names("charpentier".addWildcard())).containsExactly("Charpentier")
        assertThat(names("CHARP".addWildcard())).containsExactly("Charpentier")
        assertThat(names("cofor")).containsExactly("COFOR")
    }

    // Known gap, pinned so that a change shows, not wanted: the index folds case but not accents, so an unaccented
    // word misses an accented name unless it sounds the same.
    @Test
    suspend fun `known gap - a typed word finds an accented name only with its accents, or by its sound`() {
        val _ = store(snap("Érable"), snap("Saint-Égrève"), snap("Thones"))

        assertThat(names("érable")).containsExactly("Érable")
        assertThat(names("ÉRABLE")).containsExactly("Érable")
        assertThat(names("Égrè".addWildcard())).containsExactly("Saint-Égrève")
        assertThat(names("Thônes")).containsExactly("Thones")
        assertThat(names("erable")).isEmpty()
        assertThat(names("egre".addWildcard())).isEmpty()
    }

    // Known gap of Redis Stack 6.2, pinned per stack: it does not lowercase « É »; only a phonetic matcher hides it.
    @Test
    suspend fun `known gap - without a phonetic matcher, an accented capital is found in lower case from 7_4 on`() {
        val plain = RedisStack.view(
            model = MatchProbeSnap::class,
            fields = listOf(
                RedisIndexField(MatchProbeSnap::id.name, RedisFieldType.TAG),
                RedisIndexField(MatchProbeSnap::name.name, RedisFieldType.TEXT),
            ),
            idOf = MatchProbeSnap::id,
            settings = IndexSettings(null),
        )
        plain.initIndex()
        val _ = store(snap("Érable"))

        val found = plain.search("érable", null, null).items.map { it.name }

        assertThat(plain.search("ÉRABLE", null, null).items.map { it.name }).containsExactly("Érable")
        assertThat(plain.search("Thônes", null, null).items).isEmpty()
        if (RedisStack.isUnicodeCaseFolded) {
            assertThat(found).containsExactly("Érable")
        } else {
            assertThat(found).isEmpty()
        }
    }

    @Test
    suspend fun `a word sounding like a name finds it`() {
        val _ = store(snap("Philippe"), snap("Martin"), snap("charpentiers"))

        assertThat(names("Filipe")).containsExactly("Philippe")
        assertThat(names("charpentier")).containsExactly("charpentiers")
    }

    @Test
    suspend fun `a tag matches its whole value only, its case aside`() {
        val _ = store(
            snap("a", status = "ACTIVE"),
            snap("b", status = "active, closed"),
            snap("c", status = "inactive"),
        )

        assertThat(names(tagClause(field = "status", value = "ACTIVE"))).containsExactly("a")
        assertThat(names(tagClause(field = "status", value = "active"))).containsExactly("a")
        assertThat(names(tagClause(field = "status", value = "active, closed"))).containsExactly("b")
        assertThat(names(tagClause(field = "status", value = "closed"))).isEmpty()
    }

    @Test
    suspend fun `a numeric range holds its bounds unless excluded, negatives included`() {
        val _ = store(
            snap("low", rank = -5),
            snap("ten", rank = 10),
            snap("twenty", rank = 20),
            snap("high", rank = 99),
        )

        assertThat(names("@rank:[10 20]")).containsExactly("ten", "twenty")
        assertThat(names("@rank:[(10 20]")).containsExactly("twenty")
        assertThat(names("@rank:[-inf 0]")).containsExactly("low")
        assertThat(names("@rank:[21 +inf]")).containsExactly("high")
    }

    @Test
    suspend fun `a text clause and a tag clause narrow each other`() {
        val _ = store(snap("Érable rouge", status = "open"), snap("Érable blanc", status = "closed"))

        assertThat(names("érable ${tagClause(field = "status", value = "open")}")).containsExactly("Érable rouge")
    }
}
