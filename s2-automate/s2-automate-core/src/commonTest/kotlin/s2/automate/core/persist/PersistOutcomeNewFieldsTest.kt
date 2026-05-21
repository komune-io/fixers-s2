package s2.automate.core.persist

import s2.dsl.automate.s2error
import kotlin.test.Test
import kotlin.test.assertEquals

class PersistOutcomeNewFieldsTest {

    @Test
    fun `Success carries optional metadata map`() {
        val outcome = PersistOutcome.Success(
            msgId = "cmd-1",
            event = "EVT",
            metadata = mapOf("transactionId" to "tx", "blockNumber" to "1"),
        )
        assertEquals("tx", outcome.metadata["transactionId"])
        assertEquals("1", outcome.metadata["blockNumber"])
    }

    @Test
    fun `Success metadata defaults to emptyMap for callers that don't provide it`() {
        val outcome = PersistOutcome.Success(msgId = "c", event = "EVT")
        assertEquals(emptyMap(), outcome.metadata)
    }

    @Test
    fun `category member maps each Failure subtype to the correct ErrorCategory`() {
        assertEquals(ErrorCategory.Rejected, PersistOutcome.Rejected<String>("c", s2error("X", "y")).category)
        assertEquals(ErrorCategory.Transient, PersistOutcome.Transient<String>("c", s2error("X", "y")).category)
        assertEquals(ErrorCategory.Indeterminate, PersistOutcome.Indeterminate<String>("c", s2error("X", "y")).category)
        assertEquals(ErrorCategory.Conflict, PersistOutcome.Conflict<String>("c", s2error("X", "y")).category)
    }
}
