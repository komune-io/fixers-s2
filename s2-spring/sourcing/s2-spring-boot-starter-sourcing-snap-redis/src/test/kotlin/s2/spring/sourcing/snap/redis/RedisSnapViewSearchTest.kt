package s2.spring.sourcing.snap.redis

import f2.dsl.cqrs.exception.F2Exception
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

	private companion object {
		/** RediSearch's default `MAXEXPANSIONS`, past which a prefix stops expanding. */
		const val MAX_EXPANSIONS = 200
	}

	private val view = RedisStack.view(
		SearchProbeSnap::class,
		listOf(
			RedisIndexField(SearchProbeSnap::id.name, RedisFieldType.TAG),
			RedisIndexField(SearchProbeSnap::name.name, RedisFieldType.TEXT),
		),
		SearchProbeSnap::id,
	)

	@BeforeEach
	fun prepare() = runBlocking {
		view.deleteAll()
		view.initIndex()
	}

	@AfterEach
	fun tidy() = runBlocking {
		view.deleteAll()
		view.dropIndex()
	}

	private suspend fun store(names: List<String>): List<String> =
		names.map { name -> "search-probe-${UUID.randomUUID()}".also { view.save(SearchProbeSnap(id = it, name = name)) } }

	private suspend fun found(term: String) = view.search(term.addWildcard(), null, null).items.map { it.id }

	private fun refusal(search: suspend () -> Any?) =
		runCatching { runBlocking { search() } }.exceptionOrNull().let { it as F2Exception }.error

	@Test
	fun `a search Redis refuses leaves the connection serving`() {
		repeat(3) {
			assertThatThrownBy { runBlocking { view.search("Thônes* (74280)*", null, null) } }
				.describedAs("failure ${it + 1}")
				.isInstanceOf(RedisCommandExecutionException::class.java)
		}
		assertThat(runBlocking { view.get("absent-${UUID.randomUUID()}") }).isNull()
	}

	@Test
	suspend fun `a label carrying query syntax finds its document, and only it`() {
		val names = listOf(
			"Thônes (74280)",
			"Saint-Égrève",
			"L'Isle-d'Abeau",
			"Mont@Saint|Martin",
			"Les Écrins: \"Nord\"",
			"Mont_du_Lac",
		)
		val ids = store(names)

		names.zip(ids).forEach { (name, id) -> assertThat(found(name)).describedAs("«$name»").containsExactly(id) }
	}

	@Test
	suspend fun `a word English takes for an empty one is still searched`() {
		val ids = store(listOf("Silver - Or", "If", "Pont de pierre"))

		listOf("Or" to ids[0], "Silver - Or" to ids[0], "If" to ids[1], "de" to ids[2]).forEach { (term, id) ->
			assertThat(found(term)).describedAs("«$term»").containsExactly(id)
		}
	}

	@Test
	suspend fun `a page is sorted and bounded, its total the whole match`() {
		store(listOf("Charlie", "Alpha", "Bravo"))

		val page = view.search(null, OffsetPagination(offset = 0, limit = 2), "name", descending = true)

		assertThat(page.items.map { it.name }).containsExactly("Charlie", "Bravo")
		assertThat(page.total).isEqualTo(3)
		assertThat(view.count()).isEqualTo(3)
	}

	@Test
	fun `a page larger than one read answers is refused`() {
		val error = refusal { view.search(null, OffsetPagination(offset = 0, limit = SEARCH_WINDOW + 1), null) }

		assertThat(error.refusal).isEqualTo(SearchRefusal.PAGE_TOO_LARGE)
		assertThat(error.code).isEqualTo(422)
		assertThat(error.parameters).containsExactlyInAnyOrderEntriesOf(
			mapOf("index" to "SearchProbeSnap", "limit" to "${SEARCH_WINDOW + 1}", "bound" to "$SEARCH_WINDOW"),
		)
	}

	/** Two letters are a prefix RediSearch expands, to `MAXEXPANSIONS` terms at most, then it stops: the rest is cut. */
	@Test
	suspend fun `a prefix expanding past MAXEXPANSIONS is refused, never cut`() {
		store((0..MAX_EXPANSIONS).map { "ab${"$it".padStart(3, '0')}x" })

		val error = refusal { view.search("ab".addWildcard(), OffsetPagination(offset = 0, limit = 1), null) }

		assertThat(error.refusal).isEqualTo(SearchRefusal.TOO_BROAD)
		assertThat(error.parameters).containsEntry("warning", "Max prefix expansions limit was reached")
		assertThat(found("ab0")).hasSize(100)
	}

	@Test
	suspend fun `accented text is kept intact whatever the JVM default charset`() {
		assertThat(Charset.defaultCharset())
			.describedAs("test precondition: the test JVM must not default to UTF-8")
			.isNotEqualTo(StandardCharsets.UTF_8)
		val text = "Café du Mélézin — Saint-Égrève"
		val id = store(listOf(text)).single()

		assertThat(view.get(id)?.name).isEqualTo(text)
		assertThat(view.getAll(listOf(id)).getValue(id).name).isEqualTo(text)
		assertThat(view.search(null, null, null).items.single().name).isEqualTo(text)
	}
}
