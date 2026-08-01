package s2.bdd.data

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import s2.bdd.exception.EntityNotInitializedException

class TestEntitiesTest {

    @Test
    fun `set and get should track the last used entity`() {
        val entities = TestEntities<String, String>("order")
        entities["a"] = "entityA"

        assertThat(entities["a"]).isEqualTo("entityA")
        assertThat(entities.lastUsedKey).isEqualTo("a")
        assertThat(entities.lastUsed).isEqualTo("entityA")
        assertThat(entities.lastUsedOrNull).isEqualTo("entityA")
    }

    @Test
    fun `lastUsedKey should throw when nothing was used`() {
        val entities = TestEntities<String, String>("order")

        assertThatThrownBy { entities.lastUsedKey }
            .isInstanceOf(EntityNotInitializedException::class.java)
            .hasMessageContaining("order")
    }

    @Test
    fun `lastUsed should throw when last used entity is null`() {
        val entities = TestEntities<String, String>("order")
        entities["a"] = null

        assertThat(entities.lastUsedOrNull).isNull()
        assertThatThrownBy { entities.lastUsed }
            .isInstanceOf(EntityNotInitializedException::class.java)
    }

    @Test
    fun `safeGet should return entity or throw when absent`() {
        val entities = TestEntities<String, String>("order")
        entities["a"] = "entityA"

        assertThat(entities.safeGet("a")).isEqualTo("entityA")
        assertThatThrownBy { entities.safeGet("missing") }
            .isInstanceOf(EntityNotInitializedException::class.java)
            .hasMessageContaining("missing")
    }

    @Test
    fun `items and keys should reflect non-null entities`() {
        val entities = TestEntities<String, String>("order")
        entities["a"] = "entityA"
        entities["b"] = null

        assertThat(entities.items).containsExactly("entityA")
        assertThat(entities.keys).containsExactlyInAnyOrder("a", "b")
        assertThat(entities.size).isEqualTo(1)
        assertThat(entities.containsKey("b")).isTrue()
        assertThat(entities.containsKey("c")).isFalse()
    }

    @Test
    fun `register should store item and rethrow with null value on failure`() {
        val entities = TestEntities<String, String>("order")
        entities.register("a") { "entityA" }
        assertThat(entities["a"]).isEqualTo("entityA")

        assertThatThrownBy {
            entities.register("b") { error("creation failed") }
        }.isInstanceOf(IllegalStateException::class.java)
        assertThat(entities.containsKey("b")).isTrue()
        assertThat(entities["b"]).isNull()
    }

    @Test
    fun `putAll should store all entries from pairs and map`() {
        val entities = TestEntities<String, String>("order")
        entities.putAll("a" to "entityA", "b" to "entityB")
        entities.putAll(mapOf("c" to "entityC"))

        assertThat(entities.keys).containsExactlyInAnyOrder("a", "b", "c")
        assertThat(entities.map { it.key }).containsExactlyInAnyOrder("a", "b", "c")
    }

    @Test
    fun `reset should clear entities and last used`() {
        val entities = TestEntities<String, String>("order")
        entities["a"] = "entityA"

        entities.reset()

        assertThat(entities.items).isEmpty()
        assertThat(entities.lastUsedOrNull).isNull()
        assertThatThrownBy { entities.lastUsedKey }
            .isInstanceOf(EntityNotInitializedException::class.java)
    }
}
