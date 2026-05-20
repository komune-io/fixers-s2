package s2.automate.core.persist

import s2.dsl.automate.s2error
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PersistOutcomeTest {

    @Test
    fun `Success carries commandId, event, transactionId, blockNumber`() {
        val outcome = PersistOutcome.Success(
            commandId = "cmd-1",
            event = "EVT",
            transactionId = "tx-abc",
            blockNumber = 42L,
        )
        assertEquals("cmd-1", outcome.commandId)
        assertEquals("EVT", outcome.event)
        assertEquals("tx-abc", outcome.transactionId)
        assertEquals(42L, outcome.blockNumber)
    }

    @Test
    fun `Rejected carries commandId, error and no event`() {
        val outcome = PersistOutcome.Rejected<String>(
            commandId = "cmd-2",
            error = s2error("SESSION_NOT_FOUND", "session abc missing"),
        )
        assertEquals("cmd-2", outcome.commandId)
        assertEquals("SESSION_NOT_FOUND", outcome.error.type)
        assertEquals("session abc missing", outcome.error.description)
    }

    @Test
    fun `Transient and Conflict and Indeterminate distinguish from Rejected`() {
        val transient = PersistOutcome.Transient<String>("c", s2error("GATEWAY_5XX", "boom"))
        val conflict = PersistOutcome.Conflict<String>("c", s2error("MVCC_READ_CONFLICT", ""))
        val indeterminate = PersistOutcome.Indeterminate<String>("c", s2error("POST_SUBMIT_TIMEOUT", ""))
        assertEquals("GATEWAY_5XX", transient.error.type)
        assertEquals("MVCC_READ_CONFLICT", conflict.error.type)
        assertEquals("POST_SUBMIT_TIMEOUT", indeterminate.error.type)
    }

    @Test
    fun `successful event extraction returns null on failure variants`() {
        val rejected: PersistOutcome<String> = PersistOutcome.Rejected("c", s2error("X", "Y"))
        assertNull(rejected.eventOrNull())
        assertEquals("EVT", PersistOutcome.Success("c", "EVT", "t", 1L).eventOrNull())
    }
}
