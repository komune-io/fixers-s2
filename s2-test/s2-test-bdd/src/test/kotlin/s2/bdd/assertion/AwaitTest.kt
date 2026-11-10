package s2.bdd.assertion

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
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

    /** Under runTest a virtual delay would let the condition be polled a thousand times before the thread sets it. */
    @Test
    fun `awaitUntil waits in real time under runTest`() = runTest {
        val holds = AtomicBoolean(false)
        val setter = thread { Thread.sleep(SET_AFTER_MS).also { holds.set(true) } }
        var calls = 0

        awaitUntil(timeout = 5.seconds, interval = 10.milliseconds) { ++calls; holds.get() }

        setter.join()
        assertThat(calls).isLessThan(SET_AFTER_MS.toInt())
    }

    @Test
    fun `awaitUntil under runTest fails only once the real timeout has passed`() {
        val started = TimeSource.Monotonic.markNow()

        assertThatThrownBy { runTest { awaitUntil(timeout = 200.milliseconds, interval = 10.milliseconds) { false } } }
            .isInstanceOf(AssertionError::class.java)

        assertThat(started.elapsedNow()).isGreaterThanOrEqualTo(200.milliseconds)
    }

    private companion object {
        const val SET_AFTER_MS = 300L
    }
}
