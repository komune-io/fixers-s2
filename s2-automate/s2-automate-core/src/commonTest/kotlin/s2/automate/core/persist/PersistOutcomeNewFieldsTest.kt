package s2.automate.core.persist

import s2.dsl.automate.ErrorCategory
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
        assertEquals(expected = "tx", actual = outcome.metadata["transactionId"])
        assertEquals(expected = "1", actual = outcome.metadata["blockNumber"])
    }

    @Test
    fun `Success metadata defaults to emptyMap for callers that don't provide it`() {
        val outcome = PersistOutcome.Success(msgId = "c", event = "EVT")
        assertEquals(expected = emptyMap(), actual = outcome.metadata)
    }

    @Test
    fun `category member maps each Failure subtype to the correct ErrorCategory`() {
        assertEquals(
            expected = ErrorCategory.Rejected,
            actual = PersistOutcome.Rejected<String>("c", s2error(code = "X", description = "y")).category,
        )
        assertEquals(
            expected = ErrorCategory.Transient,
            actual = PersistOutcome.Transient<String>("c", s2error(code = "X", description = "y")).category,
        )
        assertEquals(
            expected = ErrorCategory.Indeterminate,
            actual = PersistOutcome.Indeterminate<String>("c", s2error(code = "X", description = "y")).category,
        )
        assertEquals(
            expected = ErrorCategory.Conflict,
            actual = PersistOutcome.Conflict<String>("c", s2error(code = "X", description = "y")).category,
        )
    }
}
