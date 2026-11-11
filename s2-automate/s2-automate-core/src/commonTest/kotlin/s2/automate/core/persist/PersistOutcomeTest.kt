package s2.automate.core.persist

import s2.dsl.automate.s2error
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PersistOutcomeTest {

    @Test
    fun `Success carries msgId, event, and optional metadata`() {
        val outcome = PersistOutcome.Success(
            msgId = "msg-1",
            event = "EVT",
            metadata = mapOf("transactionId" to "tx-abc"),
        )
        assertEquals(expected = "msg-1", actual = outcome.msgId)
        assertEquals(expected = "EVT", actual = outcome.event)
        assertEquals(expected = "tx-abc", actual = outcome.metadata["transactionId"])
    }

    @Test
    fun `Success metadata defaults to emptyMap`() {
        val outcome = PersistOutcome.Success(msgId = "msg-2", event = "EVT")
        assertEquals(expected = emptyMap(), actual = outcome.metadata)
    }

    @Test
    fun `Rejected carries msgId, error and no event`() {
        val outcome = PersistOutcome.Rejected<String>(
            msgId = "msg-2",
            error = s2error(code = "SESSION_NOT_FOUND", description = "session abc missing"),
        )
        assertEquals(expected = "msg-2", actual = outcome.msgId)
        assertEquals(expected = "SESSION_NOT_FOUND", actual = outcome.error.type)
        assertEquals(expected = "session abc missing", actual = outcome.error.description)
    }

    @Test
    fun `Transient and Conflict and Indeterminate distinguish from Rejected`() {
        val transient = PersistOutcome.Transient<String>("c", s2error(code = "GATEWAY_5XX", description = "boom"))
        val conflict = PersistOutcome.Conflict<String>("c", s2error(code = "MVCC_READ_CONFLICT", description = ""))
        val indeterminate = PersistOutcome.Indeterminate<String>(
            "c",
            s2error(code = "POST_SUBMIT_TIMEOUT", description = ""),
        )
        assertEquals(expected = "GATEWAY_5XX", actual = transient.error.type)
        assertEquals(expected = "MVCC_READ_CONFLICT", actual = conflict.error.type)
        assertEquals(expected = "POST_SUBMIT_TIMEOUT", actual = indeterminate.error.type)
    }

    @Test
    fun `successful event extraction returns null on failure variants`() {
        val rejected: PersistOutcome<String> = PersistOutcome.Rejected("c", s2error(code = "X", description = "Y"))
        assertNull(rejected.eventOrNull())
        assertEquals(expected = "EVT", actual = PersistOutcome.Success(msgId = "c", event = "EVT").eventOrNull())
    }
}
