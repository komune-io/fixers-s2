package s2.sample.orderbook.sourcing.core.redis

import io.lettuce.core.ClientOptions
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.protocol.ProtocolVersion
import io.lettuce.core.support.ConnectionPoolSupport
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.apache.commons.pool2.impl.GenericObjectPool
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import tools.jackson.module.kotlin.jacksonObjectMapper

data class TestEntity(
	val id: String = "",
	val name: String = "",
	val status: String = "",
)

@Testcontainers
class RedisSnapViewIntegrationTest {

	companion object {
		private const val REDIS_PORT = 6379

		@Container
		@JvmStatic
		val redisContainer: GenericContainer<*> =
			GenericContainer(DockerImageName.parse("redis/redis-stack:latest"))
				.withExposedPorts(REDIS_PORT)

		private lateinit var pool: GenericObjectPool<StatefulRedisConnection<String, String>>
		private lateinit var client: RedisClient
		lateinit var redisSnapView: RedisSnapView

		@BeforeAll
		@JvmStatic
		fun setup() {
			client = RedisClient.create(
				"redis://${redisContainer.host}:${redisContainer.getMappedPort(REDIS_PORT)}"
			)
			client.options = ClientOptions.builder()
				.protocolVersion(ProtocolVersion.RESP2)
				.build()
			val poolConfig = GenericObjectPoolConfig<StatefulRedisConnection<String, String>>()
			poolConfig.maxTotal = 4
			pool = ConnectionPoolSupport.createGenericObjectPool({ client.connect() }, poolConfig)

			redisSnapView = RedisSnapView(pool, jacksonObjectMapper())
		}

		@AfterAll
		@JvmStatic
		fun teardown() {
			pool.close()
			client.shutdown()
		}
	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
	inner class CrudOperations {
		@Test
		@Order(1)
		fun `should save and get entity`() = runBlocking {
			val entity = TestEntity(id = "1", name = "Test", status = "active")
			val saved = redisSnapView.save<TestEntity>("1", entity)

			assertEquals("Test", saved.name)
			assertEquals("active", saved.status)

			val loaded = redisSnapView.get<TestEntity>("1")
			assertNotNull(loaded)
			assertEquals("Test", loaded!!.name)
			assertEquals("active", loaded.status)
		}

		@Test
		@Order(2)
		fun `should return null for non-existent entity`() = runBlocking {
			val result = redisSnapView.get<TestEntity>("non-existent")
			assertNull(result)
		}

		@Test
		@Order(3)
		fun `should update existing entity`() = runBlocking {
			val entity = TestEntity(id = "2", name = "Original", status = "draft")
			redisSnapView.save<TestEntity>("2", entity)

			val updated = TestEntity(id = "2", name = "Updated", status = "published")
			redisSnapView.save<TestEntity>("2", updated)

			val loaded = redisSnapView.get<TestEntity>("2")
			assertNotNull(loaded)
			assertEquals("Updated", loaded!!.name)
			assertEquals("published", loaded.status)
		}

		@Test
		@Order(4)
		fun `should delete entity`() = runBlocking {
			val entity = TestEntity(id = "to-delete", name = "Delete Me", status = "active")
			redisSnapView.save<TestEntity>("to-delete", entity)

			val deleted = redisSnapView.delete<TestEntity>("to-delete")
			assertTrue(deleted)

			val loaded = redisSnapView.get<TestEntity>("to-delete")
			assertNull(loaded)
		}
	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
	inner class IndexAndSearch {
		@Test
		@Order(1)
		fun `should create index and search entities`() = runBlocking {
			redisSnapView.dropIndex<TestEntity>()
			redisSnapView.createIndex<TestEntity>(
				RedisIndexField("name", RedisFieldType.TEXT),
				RedisIndexField("status", RedisFieldType.TAG),
			)

			redisSnapView.save<TestEntity>("s1", TestEntity("s1", "Alice", "active"))
			redisSnapView.save<TestEntity>("s2", TestEntity("s2", "Bob", "active"))
			redisSnapView.save<TestEntity>("s3", TestEntity("s3", "Charlie", "closed"))

			// Wait briefly for index to catch up
			Thread.sleep(500)

			val result = redisSnapView.search<TestEntity>(query = null, pagination = null, sortBy = null)
			assertTrue(result.total >= 3, "Expected at least 3 results, got ${result.total}")
		}

		@Test
		@Order(2)
		fun `should search by tag`() = runBlocking {
			val result = redisSnapView.searchById<TestEntity>("status", "active")
			assertTrue(result.total >= 2, "Expected at least 2 active results, got ${result.total}")
		}

		@Test
		@Order(3)
		fun `should count entities`() = runBlocking {
			val count = redisSnapView.count<TestEntity>()
			assertTrue(count >= 3, "Expected at least 3, got $count")
		}

		@Test
		@Order(4)
		fun `should get all entities`() = runBlocking {
			val all = redisSnapView.all<TestEntity>().toList()
			assertTrue(all.size >= 3, "Expected at least 3, got ${all.size}")
		}

		@Test
		@Order(5)
		fun `should drop index without error`() = runBlocking {
			redisSnapView.dropIndex<TestEntity>()
			// Should not throw — verifies dropIndex works
		}

		@Test
		@Order(6)
		fun `should handle drop of non-existent index`() = runBlocking {
			// Drop again on already-dropped index — should handle gracefully
			redisSnapView.dropIndex<TestEntity>()
		}
	}
}
