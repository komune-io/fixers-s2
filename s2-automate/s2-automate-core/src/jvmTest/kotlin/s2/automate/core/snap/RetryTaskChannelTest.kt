package s2.automate.core.snap

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RetryTaskChannelTest {

    private lateinit var retryTaskChannel: RetryTaskChannel


    class RetryException(message: String) : Exception(message)

    @BeforeEach
    fun setUp() {
        retryTaskChannel = RetryTaskChannel(5, 1000, RetryException::class)
    }

    @Test
    fun `test add to persist queue`() = runTest {
        val id = "testId"
        val event = "testEvent"
        val result: Pair<String, String> = retryTaskChannel.addToPersistQueue(id, event) { evt ->
            "entity" to evt
        }
        assertEquals("entity", result.first)
        assertEquals("testEvent", result.second)
    }

    @Test
    fun `test retry on RetryException failure`() = runTest {
        val id = "testId"
        val event = "testEvent"
        val attempts = AtomicInteger(0)

        val result = retryTaskChannel.addToPersistQueue(id, event) { evt ->
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
    fun `test max retry RetryException attempts`() = runTest {
        val id = "testId"
        val event = "testEvent"
        val attempts = AtomicInteger(0)

        val exception = assertThrows<Exception> {
            retryTaskChannel.addToPersistQueue<String, String, String>(id, event) { evt ->
                attempts.incrementAndGet()
                throw RetryException("Retry $evt")
            }
        }

        assertEquals(5, attempts.get())
        assertEquals("Retry $event", exception.message)
    }

    @Test
    fun `test no retry on non-RetryException failure`() = runTest {
        val id = "testId"
        val event = "testEvent"
        val attempts = AtomicInteger(0)

        val exception = assertThrows<Exception> {
            retryTaskChannel.addToPersistQueue<String, String, String>(id, event) { evt ->
                attempts.incrementAndGet()
                throw Exception("Non-retryable exception")
            }
        }

        assertEquals(1, attempts.get())
        assertEquals("Non-retryable exception", exception.message)

    }
}