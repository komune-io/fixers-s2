package s2.bdd.assertion

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class AwaitTest {

    @Test
    fun `awaitUntil should return once the condition holds`() {
        var calls = 0

        runBlocking { awaitUntil(timeout = 1_000.milliseconds, interval = 1.milliseconds) { ++calls == 3 } }

        assertThat(calls).isEqualTo(3)
    }

    @Test
    fun `awaitUntil should fail with the description when the condition never holds`() {
        assertThatThrownBy {
            runBlocking {
                awaitUntil(timeout = 20.milliseconds, interval = 1.milliseconds, description = "order [1] is Closed") {
                    false
                }
            }
        }.isInstanceOf(AssertionError::class.java)
            .hasMessage("Timed out after 20ms waiting for order [1] is Closed")
    }

    @Test
    fun `awaitUntil should check the condition at least once`() {
        var calls = 0

        runBlocking { awaitUntil(timeout = Duration.ZERO) { ++calls == 1 } }

        assertThat(calls).isEqualTo(1)
    }
}
