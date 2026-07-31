package s2.bdd.assertion

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import s2.bdd.data.TestContext

class AssertionExceptionsTest {

    private fun contextWithErrors(vararg exceptions: Exception): TestContext {
        val context = TestContext()
        exceptions.forEach(context.errors::add)
        return context
    }

    @Test
    fun `hasBeenThrown should pass when count matches`() {
        val context = contextWithErrors(
            IllegalArgumentException("first"),
            IllegalArgumentException("second"),
            IllegalStateException("other")
        )

        AssertionBdd.exceptions(context)
            .assertThat(IllegalArgumentException::class)
            .hasBeenThrown(2)
    }

    @Test
    fun `hasBeenThrown should fail when count does not match`() {
        val context = contextWithErrors(IllegalArgumentException("first"))

        assertThatThrownBy {
            AssertionBdd.exceptions(context)
                .assertThat(IllegalArgumentException::class)
                .hasBeenThrown(2)
        }.isInstanceOf(AssertionError::class.java)
    }

    @Test
    fun `hasNotBeenThrown should pass when no matching exception`() {
        val context = contextWithErrors(IllegalStateException("other"))

        AssertionBdd.exceptions(context)
            .assertThat(IllegalArgumentException::class)
            .hasNotBeenThrown()
    }

    @Test
    fun `hasBeenThrownAtLeast and atMost should respect bounds`() {
        val context = contextWithErrors(
            IllegalArgumentException("first"),
            IllegalArgumentException("second")
        )
        val assertion = AssertionBdd.exceptions(context).assertThat(IllegalArgumentException::class)

        assertion.hasBeenThrownAtLeast(1)
        assertion.hasBeenThrownAtMost(2)
        assertThatThrownBy { assertion.hasBeenThrownAtLeast(3) }
            .isInstanceOf(AssertionError::class.java)
        assertThatThrownBy { assertion.hasBeenThrownAtMost(1) }
            .isInstanceOf(AssertionError::class.java)
    }

    @Test
    fun `matcher should filter exceptions`() {
        val context = contextWithErrors(
            IllegalArgumentException("match"),
            IllegalArgumentException("no-match")
        )

        AssertionBdd.exceptions(context)
            .assertThat(IllegalArgumentException::class)
            .hasBeenThrown(1) { it.message == "match" }
    }
}
