package s2.spring.sourcing.snap.redis

import java.util.function.Supplier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.event.ContextRefreshedEvent
import s2.spring.sourcing.snap.redis.config.RedisSnapAutoConfiguration

class RedisIndexesTest {

	private fun index(name: String, created: MutableList<String>) = RedisIndexInitializer { created += name }

	/** Scheduled tasks are registered, and runners called, once the context is refreshed. */
	@Test
	fun `every index is created once, before the context is refreshed`() {
		val created = mutableListOf<String>()
		val atRefresh = mutableListOf<List<String>>()

		AnnotationConfigApplicationContext().use { context ->
			context.registerBean("first", RedisIndexInitializer::class.java, Supplier { index("first", created) })
			context.registerBean("second", RedisIndexInitializer::class.java, Supplier { index("second", created) })
			context.register(RedisSnapAutoConfiguration::class.java)
			context.addApplicationListener(ApplicationListener<ContextRefreshedEvent> { atRefresh += created.toList() })
			context.refresh()
		}

		assertThat(atRefresh).containsExactly(listOf("first", "second"))
	}

	@Test
	suspend fun `creating them again recreates every one`() {
		val created = mutableListOf<String>()

		RedisIndexes(listOf(index("first", created), index("second", created))).create()

		assertThat(created).containsExactly("first", "second")
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
