package s2.dsl.automate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class S2ModelTest {

    enum class TestState(override val position: Int) : S2State {
        Created(0), Active(1)
    }

    data class DoCmd(override val id: String) : S2Command<String>

    @Test
    fun `s2error builds an S2ErrorBase with payload and cause`() {
        val cause = IllegalStateException("boom")
        val error = s2error(
            code = "MY_ERROR",
            description = "Something failed",
            payload = mapOf("key" to "value"),
            cause = cause,
        )
        assertThat(error.type).isEqualTo("MY_ERROR")
        assertThat(error.description).isEqualTo("Something failed")
        assertThat(error.date).isEmpty()
        assertThat(error.payload).containsEntry("key", "value")
        assertThat(error.cause).isSameAs(cause)
    }

    @Test
    fun `s2error defaults payload and cause`() {
        val error = s2error(code = "CODE", description = "desc")
        assertThat(error.payload).isEmpty()
        assertThat(error.cause).isNull()
    }

    @Test
    fun `s2warning builds an S2ErrorBase without cause`() {
        val warning = s2warning(code = "WARN", description = "careful", payload = mapOf("a" to "b"))
        assertThat(warning.type).isEqualTo("WARN")
        assertThat(warning.description).isEqualTo("careful")
        assertThat(warning.payload).containsEntry("a", "b")
        assertThat(warning.cause).isNull()
    }

    @Test
    fun `S2ErrorBase toString contains its fields`() {
        val error = s2error(code = "CODE", description = "desc", payload = mapOf("k" to "v"))
        assertThat(error.toString())
            .contains("CODE")
            .contains("desc")
            .contains("k")
    }

    @Test
    fun `S2EventSuccess carries id command and states`() {
        val cmd = DoCmd("42")
        val event = S2EventSuccess(id = "42", type = cmd, from = TestState.Created, to = TestState.Active)
        assertThat(event.id).isEqualTo("42")
        assertThat(event.type).isSameAs(cmd)
        assertThat(event.from).isEqualTo(TestState.Created)
        assertThat(event.to).isEqualTo(TestState.Active)
    }

    @Test
    fun `S2EventError carries the error alongside the transition`() {
        val cmd = DoCmd("42")
        val error = s2error(code = "ERR", description = "failed")
        val event = S2EventError(id = "42", type = cmd, from = TestState.Created, to = TestState.Active, error = error)
        assertThat(event.id).isEqualTo("42")
        assertThat(event.error.type).isEqualTo("ERR")
        assertThat(event.from).isEqualTo(TestState.Created)
        assertThat(event.to).isEqualTo(TestState.Active)
    }

    @Test
    fun `S2SubMachine defaults its flags`() {
        val automate = S2Automate(name = "Sub", version = null, transitions = emptyArray())
        val sub = S2SubMachine(automate = automate)
        assertThat(sub.automate).isSameAs(automate)
        assertThat(sub.startsOn).isEmpty()
        assertThat(sub.endsOn).isEmpty()
        assertThat(sub.isAutostart).isFalse()
        assertThat(sub.isBlocking).isFalse()
        assertThat(sub.isSingleton).isFalse()
    }

    @Test
    fun `S2SubMachine accepts explicit configuration`() {
        val automate = S2Automate(name = "Sub", version = "1", transitions = emptyArray())
        val sub = S2SubMachine(
            automate = automate,
            startsOn = listOf(DoCmd::class),
            endsOn = listOf(DoCmd::class),
            isAutostart = true,
            isBlocking = true,
            isSingleton = true,
        )
        assertThat(sub.startsOn).containsExactly(DoCmd::class)
        assertThat(sub.endsOn).containsExactly(DoCmd::class)
        assertThat(sub.isAutostart).isTrue()
        assertThat(sub.isBlocking).isTrue()
        assertThat(sub.isSingleton).isTrue()
    }

    @Test
    fun `ErrorCategory exposes the four retry categories`() {
        assertThat(ErrorCategory.entries.map { it.name }).containsExactly(
            "Rejected", "Transient", "Indeterminate", "Conflict",
        )
    }
}
