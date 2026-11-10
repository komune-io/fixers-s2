package s2.spring.sourcing.snap.redis

import java.util.function.Supplier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.event.ContextRefreshedEvent
import s2.spring.sourcing.snap.redis.config.RedisSnapAutoConfiguration

class RedisIndexesTest {

	private class Index(
		override val indexName: String,
		private val created: MutableList<String> = mutableListOf(),
		private val made: suspend () -> Unit = {},
	) : RedisIndexInitializer {
		override suspend fun initIndex() {
			made()
			created += indexName
		}
	}

	/** Scheduled tasks are registered, and runners called, once the context is refreshed. */
	@Test
	fun `every index is created once, before the context is refreshed`() {
		val created = mutableListOf<String>()
		val atRefresh = mutableListOf<List<String>>()

		AnnotationConfigApplicationContext().use { context ->
			context.registerBean("first", RedisIndexInitializer::class.java, Supplier { Index("first", created) })
			context.registerBean("second", RedisIndexInitializer::class.java, Supplier { Index("second", created) })
			context.register(RedisSnapAutoConfiguration::class.java)
			context.addApplicationListener(ApplicationListener<ContextRefreshedEvent> { atRefresh += created.toList() })
			context.refresh()
		}

		assertThat(atRefresh).containsExactly(listOf("first", "second"))
	}

	@Test
	suspend fun `creating them again recreates every one`() {
		val created = mutableListOf<String>()
		val indexes = RedisIndexes(listOf(Index("first", created), Index("second", created)))

		indexes.create()
		indexes.create()

		assertThat(created).containsExactly("first", "second", "first", "second")
	}

	/** The first index waits for the second to start: made one after the other, the start would never end. */
	@Test
	suspend fun `every index is made at once, not one after the other`() {
		val secondStarted = CompletableDeferred<Unit>()
		val created = mutableListOf<String>()

		RedisIndexes(
			listOf(
				Index("first", created) { secondStarted.await() },
				Index("second", created) { secondStarted.complete(Unit) },
			),
		).create()

		assertThat(created).containsExactly("second", "first")
	}

	@Test
	fun `a view that cannot make its index fails the start`() {
		val failure = IllegalStateException("Redis gone")

		assertThatThrownBy { runBlocking { RedisIndexes(listOf(Index("first") { throw failure })).create() } }
			.isInstanceOf(IllegalStateException::class.java)
			.hasMessage("Redis gone")
	}

	@Test
	fun `two views of models sharing a name are refused`() {
		assertThatThrownBy { RedisIndexes(listOf(Index("Model"), Index("Other"), Index("Model"))) }
			.isInstanceOf(IllegalArgumentException::class.java)
			.hasMessageContaining("[Model]")
	}

	@Test
	fun `the auto-configuration provides the indexes, and yields to the application's own`() {
		val runner = ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(RedisSnapAutoConfiguration::class.java))
		val own = RedisIndexes(emptyList())

		runner.run { assertThat(it).hasSingleBean(RedisIndexes::class.java).hasBean("redisIndexes") }
		runner.withBean(RedisIndexes::class.java, Supplier { own }).run {
			assertThat(it).getBean(RedisIndexes::class.java).isSameAs(own)
		}
	}
}
