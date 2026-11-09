package s2.spring.sourcing.snap.redis

import f2.dsl.cqrs.page.OffsetPagination
import io.lettuce.core.RedisCommandExecutionException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Searches through the index, on text that carries query syntax, stopwords and accents. */
class RedisSnapViewSearchTest {

	data class SearchProbeSnap(val id: String, val name: String)

	private val view = RedisStack.view()

	@BeforeEach
	suspend fun prepare() {
		view.deleteAll<SearchProbeSnap>()
		view.createIndex<SearchProbeSnap>()
	}

	@AfterEach
	suspend fun tidy() {
		view.deleteAll<SearchProbeSnap>()
		view.dropIndex<SearchProbeSnap>()
	}

	private suspend fun store(names: List<String>): List<String> =
		names.map { name ->
			"search-probe-${UUID.randomUUID()}".also { view.save(it, SearchProbeSnap(id = it, name = name)) }
		}

	private suspend fun found(term: String) =
		view.search<SearchProbeSnap>(term.addWildcard(), null, null).items.map { it.id }

	@Test
	fun `a search Redis refuses leaves the connection serving`() {
		repeat(3) {
			assertThatThrownBy { runBlocking { view.search<SearchProbeSnap>("Thônes* (74280)*", null, null) } }
				.describedAs("failure ${it + 1}")
				.isInstanceOf(RedisCommandExecutionException::class.java)
		}
		assertThat(runBlocking { view.get<SearchProbeSnap>("absent-${UUID.randomUUID()}") }).isNull()
	}

	@Test
	suspend fun `a label carrying query syntax finds its document`() {
		val names = listOf(
			"Thônes (74280)",
			"Saint-Égrève",
			"L'Isle-d'Abeau",
			"Mont@Saint|Martin",
			"Les Écrins: \"Nord\"",
			"Mont_du_Lac",
		)
		val ids = store(names)

		names.zip(ids).forEach { (name, id) -> assertThat(found(name)).describedAs("«$name»").contains(id) }
	}

	@Test
	suspend fun `a word English takes for an empty one is still searched`() {
		val ids = store(listOf("Silver - Or", "If", "Pont de pierre"))

		listOf("Or" to ids[0], "Silver - Or" to ids[0], "If" to ids[1], "de" to ids[2]).forEach { (term, id) ->
			assertThat(found(term)).describedAs("«$term»").contains(id)
		}
	}

	@Test
	suspend fun `a page is sorted and bounded, its total the whole match`() {
		store(listOf("Charlie", "Alpha", "Bravo"))

		val page = view.search<SearchProbeSnap>(null, OffsetPagination(offset = 0, limit = 2), "name", descending = true)

		assertThat(page.items.map { it.name }).containsExactly("Charlie", "Bravo")
		assertThat(page.total).isEqualTo(3)
		assertThat(view.count<SearchProbeSnap>()).isEqualTo(3)
	}

	@Test
	suspend fun `accented text is kept intact whatever the JVM default charset`() {
		assertThat(Charset.defaultCharset())
			.describedAs("test precondition: the test JVM must not default to UTF-8")
			.isNotEqualTo(StandardCharsets.UTF_8)
		val text = "Café du Mélézin — Saint-Égrève"
		val id = store(listOf(text)).single()

		assertThat(view.get<SearchProbeSnap>(id)?.name).isEqualTo(text)
		assertThat(view.getAll<SearchProbeSnap>(listOf(id)).getValue(id).name).isEqualTo(text)
		assertThat(view.search<SearchProbeSnap>(null, null, null).items.single().name).isEqualTo(text)
	}

	open class Named(val name: String)

	class Labelled(name: String, val label: String, val rank: Int) : Named(name)

	@Test
	fun `the TEXT index of a model takes its String properties, inherited ones included`() {
		assertThat(view.textFields(Labelled::class).map { it.field }).containsExactlyInAnyOrder("label", "name")
	}
}
