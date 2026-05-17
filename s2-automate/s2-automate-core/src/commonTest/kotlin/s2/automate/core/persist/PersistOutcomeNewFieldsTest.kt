package s2.automate.core.persist

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
    fun `Failure carries errorClass and errorOrigin with sensible defaults`() {
        val rejected = PersistOutcome.Rejected<String>(
            commandId = "c", errorCode = "X", errorMessage = "y",
        )
        assertEquals(ErrorClass.UNKNOWN, rejected.errorClass)
        assertEquals(ErrorOrigin.UNKNOWN, rejected.errorOrigin)
    }

    @Test
    fun `Failure can be constructed with explicit errorClass and errorOrigin`() {
        val transient = PersistOutcome.Transient<String>(
            commandId = "c", errorCode = "GRPC_UNAVAILABLE", errorMessage = "boom",
            errorClass = ErrorClass.NETWORK, errorOrigin = ErrorOrigin.FABRIC,
        )
        assertEquals(ErrorClass.NETWORK, transient.errorClass)
        assertEquals(ErrorOrigin.FABRIC, transient.errorOrigin)
    }

    @Test
    fun `Conflict and Indeterminate also carry the new fields`() {
        val conflict = PersistOutcome.Conflict<String>(
            commandId = "c", errorCode = "MVCC_READ_CONFLICT", errorMessage = "",
            errorClass = ErrorClass.STATE, errorOrigin = ErrorOrigin.FABRIC,
        )
        assertEquals(ErrorClass.STATE, conflict.errorClass)
        val indet = PersistOutcome.Indeterminate<String>(
            commandId = "c", errorCode = "POST_SUBMIT_TIMEOUT", errorMessage = "",
            errorClass = ErrorClass.NETWORK, errorOrigin = ErrorOrigin.C2_SDK,
        )
        assertEquals(ErrorOrigin.C2_SDK, indet.errorOrigin)
    }
}
