package s2.automate.core.snap

import java.util.concurrent.atomic.AtomicInteger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import s2.automate.core.storing.snap.RetryTaskChannel

class RetryTaskChannelTest {

    private lateinit var retryTaskChannel: RetryTaskChannel


    class RetryException(message: String) : Exception(message)

    @BeforeEach
    fun setUp() {
        retryTaskChannel = RetryTaskChannel(5, 1000, RetryException::class)
    }

    @Test
    suspend fun `test add to persist queue`() {
        val event = "testEvent"
        val result: Pair<String, String> = retryTaskChannel.addToPersistQueue(event) { evt ->
            "entity" to evt
        }
        assertEquals("entity", result.first)
        assertEquals("testEvent", result.second)
    }

    @Test
    suspend fun `test retry on RetryException failure`() {
        val event = "testEvent"
        val attempts = AtomicInteger(0)

        val result = retryTaskChannel.addToPersistQueue(event) { evt ->
            if (attempts.incrementAndGet() < 3) {
                throw RetryException("Retry")
            }
            "entity" to evt
        }

        assertEquals(3, attempts.get())
        assertEquals("entity", result.first)
        assertEquals("testEvent", result.second)
    }

    @Test
    suspend fun `test max retry RetryException attempts`() {
        val event = "testEvent"
        val attempts = AtomicInteger(0)

        val exception = assertThrows<Exception> {
            retryTaskChannel.addToPersistQueue<String, String>(event) { evt ->
                attempts.incrementAndGet()
                throw RetryException("Retry $evt")
            }
        }

        assertEquals(5, attempts.get())
        assertEquals("Retry $event", exception.message)
    }

    @Test
    suspend fun `test no retry on non-RetryException failure`() {
        val event = "testEvent"
        val attempts = AtomicInteger(0)

        val exception = assertThrows<Exception> {
            retryTaskChannel.addToPersistQueue<String, String>(event) { _ ->
                attempts.incrementAndGet()
                throw IllegalStateException("Non-retryable exception")
            }
        }

        assertEquals(1, attempts.get())
        assertEquals("Non-retryable exception", exception.message)

    }
}
