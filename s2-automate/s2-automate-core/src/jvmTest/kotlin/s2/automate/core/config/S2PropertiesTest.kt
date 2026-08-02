package s2.automate.core.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.asBatch

class S2PropertiesTest {

    @Test
    fun `S2RetryTaskProperties has sensible defaults`() {
        val properties = S2RetryTaskProperties()
        assertEquals(5, properties.maxAttempts)
        assertEquals(1000L, properties.delayMillis)
        val custom = S2RetryTaskProperties(maxAttempts = 2, delayMillis = 10)
        assertEquals(2, custom.maxAttempts)
        assertEquals(10L, custom.delayMillis)
    }

    @Test
    fun `asBatch maps size and concurrency`() {
        val batch = S2BatchProperties(size = 7, concurrency = 3).asBatch()
        assertEquals(7, batch.size)
        assertEquals(3, batch.concurrency)
    }

    @Test
    fun `AutomateContext exposes automate and batch`() {
        val properties = S2BatchProperties(size = 1, concurrency = 1)
        val context = AutomateContext(automate = "AUTOMATE", batch = properties)
        assertEquals("AUTOMATE", context.automate)
        assertEquals(properties, context.batch)
    }
}
