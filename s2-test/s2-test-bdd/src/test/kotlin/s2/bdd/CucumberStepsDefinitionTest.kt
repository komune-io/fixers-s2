package s2.bdd

import kotlinx.coroutines.reactor.ReactorContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.security.core.context.SecurityContext
import reactor.core.publisher.Mono
import s2.automate.core.error.AutomateException
import s2.bdd.auth.AuthedUser
import s2.bdd.data.TestContext
import s2.dsl.automate.s2error

class CucumberStepsDefinitionTest {

    private class Steps(
        public override val context: TestContext = TestContext(),
    ) : CucumberStepsDefinition() {
        fun random(value: String?) = value.orRandom()
        fun nullValue(value: String) = value.parseNullValue()
        fun nullableOrDefault(value: String?, default: String?) = value.parseNullableOrDefault(default)
        fun <T> nullableOrDefault(value: String?, default: T?, parse: (String) -> T?) =
            value.parseNullableOrDefault(default, parse)

        fun runStep(block: suspend () -> Unit) = step(block)
        fun runStep(propagate: (Exception) -> Boolean, block: suspend () -> Unit) = step(propagate, block)
        fun authed() = authedContext()
    }

    @Test
    fun `orRandom keeps the value and generates one when null`() {
        val steps = Steps()
        assertThat(steps.random("fixed")).isEqualTo("fixed")
        assertThat(steps.random(null)).isNotBlank()
        assertThat(steps.random(null)).isNotEqualTo(steps.random(null))
    }

    @Test
    fun `parseNullValue turns the null literal into null`() {
        val steps = Steps()
        assertThat(steps.nullValue("null")).isNull()
        assertThat(steps.nullValue("value")).isEqualTo("value")
    }

    @Test
    fun `parseNullableOrDefault handles null literal and default`() {
        val steps = Steps()
        assertThat(steps.nullableOrDefault(null, "default")).isEqualTo("default")
        assertThat(steps.nullableOrDefault("null", "default")).isNull()
        assertThat(steps.nullableOrDefault("7", 0) { it.toInt() }).isEqualTo(7)
    }

    @Test
    fun `step runs the block in an authenticated coroutine`() {
        val steps = Steps()
        var executed = false
        steps.runStep { executed = true }
        assertThat(executed).isTrue()
    }

    @Test
    fun `step records an AutomateException without propagating it`() {
        val steps = Steps()
        steps.runStep {
            throw AutomateException(listOf(s2error("CODE", "boom")))
        }
        assertThat(steps.context.errors.lastOfType(AutomateException::class)).isNotNull()
    }

    @Test
    fun `step records and rethrows unexpected exceptions`() {
        val steps = Steps()
        assertThatThrownBy {
            steps.runStep { throw IllegalStateException("unexpected") }
        }.isInstanceOf(IllegalStateException::class.java)
        assertThat(steps.context.errors.lastOfType(IllegalStateException::class)).isNotNull()
    }

    @Test
    fun `step honors a custom propagation predicate`() {
        val steps = Steps()
        steps.runStep({ false }) { throw IllegalStateException("swallowed") }
        assertThat(steps.context.errors.lastOfType(IllegalStateException::class)).isNotNull()
    }

    @Test
    fun `authedContext is empty without an authenticated user`() {
        val steps = Steps()
        val reactorContext: ReactorContext = steps.authed()
        @Suppress("UNCHECKED_CAST")
        val securityContext =
            reactorContext.context.get<Mono<SecurityContext>>(SecurityContext::class.java)
        assertThat(securityContext.blockOptional()).isEmpty()
    }

    @Test
    fun `authedContext builds a jwt authentication for the authed user`() {
        val steps = Steps()
        steps.context.authedUser = AuthedUser(id = "user-1", memberOf = "org-1", roles = arrayOf("admin"))
        val reactorContext = steps.authed()
        @Suppress("UNCHECKED_CAST")
        val securityContext =
            reactorContext.context.get<Mono<SecurityContext>>(SecurityContext::class.java)
        val authentication = securityContext.block()!!.authentication!!
        assertThat(authentication.authorities.map { it.authority }).containsExactly("ROLE_admin")
    }
}
