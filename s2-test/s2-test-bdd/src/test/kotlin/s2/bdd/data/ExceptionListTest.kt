package s2.bdd.data

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ExceptionListTest {

    @Test
    fun `list should expose added exceptions in order`() {
        val exceptions = ExceptionList()
        val first = IllegalArgumentException("first")
        val second = IllegalStateException("second")

        exceptions.add(first)
        exceptions.add(second)

        assertThat(exceptions.list).containsExactly(first, second)
    }

    @Test
    fun `lastOfType should return the last exception of the given type`() {
        val exceptions = ExceptionList()
        val first = IllegalArgumentException("first")
        val second = IllegalArgumentException("second")
        exceptions.add(first)
        exceptions.add(IllegalStateException("other"))
        exceptions.add(second)

        assertThat(exceptions.lastOfType(IllegalArgumentException::class)).isSameAs(second)
    }

    @Test
    fun `lastOfType should return null when no exception of the given type exists`() {
        val exceptions = ExceptionList()
        exceptions.add(IllegalStateException("other"))

        assertThat(exceptions.lastOfType(IllegalArgumentException::class)).isNull()
    }

    @Test
    fun `filterIsInstance should return only exceptions of the given type`() {
        val exceptions = ExceptionList()
        val first = IllegalArgumentException("first")
        val other = IllegalStateException("other")
        exceptions.add(first)
        exceptions.add(other)

        assertThat(exceptions.filterIsInstance(IllegalArgumentException::class)).containsExactly(first)
    }

    @Test
    fun `reset should clear all exceptions`() {
        val exceptions = ExceptionList()
        exceptions.add(IllegalArgumentException("first"))

        exceptions.reset()

        assertThat(exceptions.list).isEmpty()
    }
}
