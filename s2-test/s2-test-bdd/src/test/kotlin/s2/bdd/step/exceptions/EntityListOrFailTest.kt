package s2.bdd.step.exceptions

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import s2.bdd.data.TestContext

class EntityListOrFailTest {

    @Test
    fun `entityListOrFail returns the registered entity list`() {
        val context = TestContext()
        val entities = context.testEntities<String, String>("order")
        entities["key-1"] = "entity"
        val found = context.entityListOrFail("order")
        assertThat(found).isSameAs(entities)
        assertThat(found.lastUsedKey).isEqualTo("key-1")
    }

    @Test
    fun `entityListOrFail fails with a clear message for unknown lists`() {
        val context = TestContext()
        assertThatThrownBy { context.entityListOrFail("unknown") }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("unknown")
    }
}
