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

    @Test
    suspend fun `a typed word finds a name whatever the accents and case of either`() {
        val _ = store(snap("Érable"), snap("Saint-Égrève"), snap("Thones"), snap("Thônes-Village"))

        listOf("erable", "Erable", "érable", "ÉRABLE", "ERABLE", "e\u0301rable").forEach { query ->
            assertThat(names(query)).describedAs("«$query»").containsExactly("Érable")
        }
        assertThat(names("egre".addWildcard())).containsExactly("Saint-Égrève")
        assertThat(names("ÉGRÈ".addWildcard())).containsExactly("Saint-Égrève")
        assertThat(names("thones")).containsExactly("Thones", "Thônes-Village")
        assertThat(names("THÔNES")).containsExactly("Thones", "Thônes-Village")
    }

    @Test
    suspend fun `a phrase, a prefix and an attribute clause are read without accents or case`() {
        val _ = store(snap("Scierie de l'Érable"), snap("Érable rouge"), snap("Rouge érable"))

        assertThat(names("\"érable rouge\"")).containsExactly("Érable rouge")
        assertThat(names("\"ERABLE ROUGE\"")).containsExactly("Érable rouge")
        assertThat(names("@name:(Éra* rouge)")).containsExactly("Rouge érable", "Érable rouge")
        assertThat(names("@name:SCIERIE")).containsExactly("Scierie de l'Érable")
        assertThat(names("-rouge @name:erable")).containsExactly("Scierie de l'Érable")
    }

    @Test
    suspend fun `without a phonetic matcher, every Redis Stack folds accents and capitals beyond ASCII`() {
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
        val _ = store(snap("Érable"), snap("Œuvre Ørsted"))

        suspend fun found(query: String) = plain.search(query, null, null).items.map { it.name }

        listOf("érable", "ÉRABLE", "erable", "Erable").forEach { query ->
            assertThat(found(query)).describedAs("«$query»").containsExactly("Érable")
        }
        listOf("œuvre", "ŒUVRE", "ørsted", "ØRSTED").forEach { query ->
            assertThat(found(query)).describedAs("«$query»").containsExactly("Œuvre Ørsted")
        }
        assertThat(found("Thônes")).isEmpty()
    }

    @Test
    suspend fun `a name the index finds by its words is the one IndexWords holds`() {
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
        val stored = listOf("Érable", "Saint-Égrève", "Thônes", "Œuvre Ørsted", "COFOR", "L'Isle-d'Abeau", "Lot a_b")
        val _ = store(*stored.map { snap(it) }.toTypedArray())
        val terms = listOf("erable", "ÉRABLE", "egre", "EGREVE", "thon", "œuv", "ØRST", "cof", "isle", "a_b", "b", "d")

        terms.forEach { term ->
            val found = plain.search(plain.wildcard(term), null, null).items.map { it.name }

            assertThat(found).describedAs("«$term»")
                .containsExactlyInAnyOrderElementsOf(stored.filter { IndexWords.holds(name = it, term = term) })
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
    suspend fun `a tag keeps its accents, a text query aside`() {
        val _ = store(snap("a", status = "Égrève"), snap("b", status = "egreve"))

        assertThat(names(tagClause(field = "status", value = "Égrève"))).containsExactly("a")
        assertThat(names(tagClause(field = "status", value = "egreve"))).containsExactly("b")
        assertThat(names("a ${tagClause(field = "status", value = "Égrève")}")).containsExactly("a")
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
