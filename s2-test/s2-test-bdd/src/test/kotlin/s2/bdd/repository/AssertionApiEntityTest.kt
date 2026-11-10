package s2.bdd.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AssertionApiEntityTest {

    data class Entity(val id: String, val name: String)

    private class EntityAsserter(val entity: Entity) {
        fun hasName(name: String) = apply { assertThat(entity.name).isEqualTo(name) }
    }

    private class ApiAssertion(
        private val store: Map<String, Entity>,
    ) : AssertionApiEntity<Entity, String, EntityAsserter>() {
        override suspend fun assertThat(entity: Entity) = EntityAsserter(entity)
        override suspend fun findById(id: String): Entity? = store[id]
    }

    private val assertion = ApiAssertion(mapOf("1" to Entity(id = "1", name = "one")))

    @Test
    suspend fun `exists passes when the entity is found`() {
        assertion.exists("1")
    }

    @Test
    suspend fun `exists fails when the entity is missing`() {
        var hasFailed = false
        try {
            assertion.exists("missing")
        } catch (expected: AssertionError) {
            hasFailed = true
        }
        assertThat(hasFailed).isTrue()
    }

    @Test
    suspend fun `notExists passes when the entity is missing`() {
        assertion.notExists("missing")
    }

    @Test
    suspend fun `notExists fails when the entity is found`() {
        var hasFailed = false
        try {
            assertion.notExists("1")
        } catch (expected: AssertionError) {
            hasFailed = true
        }
        assertThat(hasFailed).isTrue()
    }

    @Test
    suspend fun `assertThatId returns the entity asserter`() {
        val _ = assertion.assertThatId("1").hasName("one")
    }
}
