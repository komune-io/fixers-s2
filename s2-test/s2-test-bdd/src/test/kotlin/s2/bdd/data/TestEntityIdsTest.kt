package s2.bdd.data

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestEntityIdsTest {

    @Test
    fun `plusAssign should add id and track last created`() {
        val ids = TestEntityIds<String>()
        ids += "a"
        ids += "b"

        assertThat(ids.ids).containsExactlyInAnyOrder("a", "b")
        assertThat(ids.lastCreated).isEqualTo("b")
        assertThat("a" in ids).isTrue()
        assertThat("c" in ids).isFalse()
    }

    @Test
    fun `add and addAll should register ids`() {
        val ids = TestEntityIds<Int>()
        ids.add(1)
        ids.addAll(listOf(2, 3))

        assertThat(ids.ids).containsExactlyInAnyOrder(1, 2, 3)
        assertThat(ids.lastCreated).isEqualTo(3)
    }

    @Test
    fun `lastCreated should be null when empty`() {
        assertThat(TestEntityIds<String>().lastCreated).isNull()
        assertThat(TestEntityIds<String>().ids).isEmpty()
    }
}
