package s2.automate.core.persist

import s2.dsl.automate.s2error
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PersistOutcomeNewFieldsTest {

    @Test
    fun `Success carries optional payload`() {
        val outcome = PersistOutcome.Success(
            commandId = "cmd-1", event = "EVT", transactionId = "tx", blockNumber = 1L,
            payload = byteArrayOf(0x42),
        )
        assertContentEquals(byteArrayOf(0x42), outcome.payload)
    }

    @Test
    fun `Success payload defaults to null for callers that don't provide it`() {
        val outcome = PersistOutcome.Success("c", "EVT", "tx", 1L)
        assertNull(outcome.payload)
    }

    @Test
    fun `category member maps each Failure subtype to the correct ErrorCategory`() {
        assertEquals(ErrorCategory.Rejected, PersistOutcome.Rejected<String>("c", s2error("X", "y")).category)
        assertEquals(ErrorCategory.Transient, PersistOutcome.Transient<String>("c", s2error("X", "y")).category)
        assertEquals(ErrorCategory.Indeterminate, PersistOutcome.Indeterminate<String>("c", s2error("X", "y")).category)
        assertEquals(ErrorCategory.Conflict, PersistOutcome.Conflict<String>("c", s2error("X", "y")).category)
    }
}
