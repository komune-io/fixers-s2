package s2.sample.orderbook.sourcing.core.redis

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RedisSnapViewTest {

	@Nested
	inner class IdQueryTest {
		@Test
		fun `should build tag query for simple id`() {
			assertEquals("@name:{abc}", id("name", "abc"))
		}

		@Test
		fun `should escape hyphens in id`() {
			assertEquals("@field:{123\\-456\\-789}", id("field", "123-456-789"))
		}

		@Test
		fun `should handle id without hyphens`() {
			assertEquals("@id:{simple}", id("id", "simple"))
		}
	}

	@Nested
	inner class AddWildcardTest {
		@Test
		fun `should append wildcard to single word`() {
			assertEquals("hello*", "hello".addWildcard())
		}

		@Test
		fun `should append wildcard to each word`() {
			assertEquals("hello* world*", "hello world".addWildcard())
		}

		@Test
		fun `should return empty for null`() {
			assertEquals("", (null as String?).addWildcard())
		}

		@Test
		fun `should return empty for blank string`() {
			assertEquals("", "   ".addWildcard())
		}

		@Test
		fun `should trim and add wildcards`() {
			assertEquals("a* b*", " a  b ".addWildcard())
		}
	}

	@Nested
	inner class RedisIndexFieldTest {
		@Test
		fun `should create field with default name`() {
			val field = RedisIndexField("myField", RedisFieldType.TEXT)
			assertEquals("myField", field.field)
			assertEquals(RedisFieldType.TEXT, field.type)
			assertEquals(null, field.name)
		}

		@Test
		fun `should create field with custom name`() {
			val field = RedisIndexField("myField", RedisFieldType.TAG, "alias")
			assertEquals("myField", field.field)
			assertEquals(RedisFieldType.TAG, field.type)
			assertEquals("alias", field.name)
		}

		@Test
		fun `should support all field types`() {
			RedisFieldType.entries.forEach { type ->
				val field = RedisIndexField("f", type)
				assertEquals(type, field.type)
			}
		}
	}
}
