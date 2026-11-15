package s2.spring.sourcing.snap.redis

import io.lettuce.core.api.reactive.RedisReactiveCommands
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.json.JsonPath
import io.lettuce.core.output.StatusOutput
import io.lettuce.core.protocol.CommandArgs
import java.util.UUID
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Snapshots stored before texts were folded are found without their accents after the next start, unreloaded. */
class RedisSnapViewTextUpgradeTest {

    data class UpgradeProbeSnap(val id: String, val name: String? = null, val aliases: List<String> = emptyList())

    private val view = RedisStack.view(
        model = UpgradeProbeSnap::class,
        fields = listOf(
            RedisIndexField(UpgradeProbeSnap::id.name, RedisFieldType.TAG),
            RedisIndexField(UpgradeProbeSnap::name.name, RedisFieldType.TEXT),
            RedisIndexField.array(UpgradeProbeSnap::aliases.name, RedisFieldType.TEXT),
        ),
        idOf = UpgradeProbeSnap::id,
    )

    private val commands = RedisStack.connection.commands(RedisReactiveCommands.factory())

    @BeforeEach
    fun prepare() {
        runBlocking {
            view.dropIndex()
            val _ = view.deleteAll()
        }
    }

    @AfterEach
    fun tidy() {
        runBlocking {
            view.dropIndex()
            val _ = view.deleteAll()
        }
    }

    @Test
    suspend fun `snapshots stored under the previous index are found unaccented at the next start, arrays included`() {
        val created = commands.dispatch<String>(
            SearchCommand.FT_CREATE,
            StatusOutput(StringCodec.UTF8),
            PREVIOUS_DEFINITION.split(" ").fold(CommandArgs(StringCodec.UTF8).add(view.indexName)) { args, argument ->
                args.add(argument)
            },
        ).awaitFirst()
        assertThat(created).isEqualTo("OK")
        val erable = storeRaw(name = "Érable", aliases = listOf("Acer", "Plane"))
        val egreve = storeRaw(name = "Saint-Égrève", aliases = listOf("Grenoble"))

        view.initIndex()

        assertThat(view.count()).isEqualTo(2)
        assertThat(ids("erable")).containsExactly(erable)
        assertThat(ids("egre".addWildcard())).containsExactly(egreve)
        assertThat(ids("@aliases:plane")).containsExactly(erable)
        assertThat(view.get(erable)?.name).isEqualTo("Érable")
    }

    @Test
    fun `saves running while the start folds the stored texts keep the text of what they saved`(): Unit = runBlocking {
        val ids = (1..CONCURRENT).map { storeRaw(name = "Avant $it", aliases = listOf("x", "y")) }

        coroutineScope {
            launch { view.initIndex() }
            ids.forEachIndexed { i, id ->
                launch { val _ = view.save(UpgradeProbeSnap(id = id, name = "Après $i", aliases = listOf("Ü $i"))) }
            }
        }

        ids.forEachIndexed { i, id ->
            assertThat(folded(id, "name")).describedAs(id).isEqualTo("apres $i")
            assertThat(folded(id, "aliases")).describedAs(id).isEqualTo("u $i")
        }
        assertThat(ids("apres")).hasSize(CONCURRENT)
    }

    private suspend fun ids(query: String) = view.search(query, null, null).items.map { it.id }

    private suspend fun storeRaw(name: String, aliases: List<String>): String {
        val id = "upgrade-probe-${UUID.randomUUID()}"
        val aliasJson = aliases.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
        val answer = commands.jsonSet(
            view.key(id),
            JsonPath.of("$"),
            """{"id":"$id","name":"$name","aliases":$aliasJson,"type":"${view.indexName}"}""",
        ).awaitSingle()
        assertThat(answer).isEqualTo("OK")
        return id
    }

    private suspend fun folded(id: String, attribute: String): String =
        commands.jsonGet(view.key(id), JsonPath.of("$.__s2_text.$attribute")).awaitFirst().toString()
            .trim('[', ']', '"')

    private companion object {
        const val CONCURRENT = 50

        // The definition views wrote before texts were folded: each text on its own path, no marker.
        const val PREVIOUS_DEFINITION = "ON JSON PREFIX 1 UpgradeProbeSnap- STOPWORDS 0 SCHEMA " +
            "$.id AS id TAG $.name AS name TEXT NOSTEM PHONETIC dm:fr SORTABLE " +
            "$.aliases[*] AS aliases TEXT NOSTEM PHONETIC dm:fr SORTABLE " +
            "$.__s2_sort[\"name\"] AS __s2_sort_name TEXT NOINDEX SORTABLE UNF"
    }
}
