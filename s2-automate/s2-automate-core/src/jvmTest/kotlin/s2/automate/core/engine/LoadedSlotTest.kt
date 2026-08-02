package s2.automate.core.engine

import f2.dsl.cqrs.envelope.asEnvelopeWithType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import s2.automate.core.persist.LoadOutcome
import s2.automate.core.persist.PersistOutcome
import s2.dsl.automate.ErrorCategory
import s2.dsl.automate.s2error

class LoadedSlotTest {

    @Test
    fun `LoadOutcome variants carry their category`() {
        val error = s2error("ERR", "desc")
        assertEquals(ErrorCategory.Rejected, LoadOutcome.Rejected<String, String>("1", error).category)
        assertEquals(ErrorCategory.Transient, LoadOutcome.Transient<String, String>("1", error).category)
        assertEquals(ErrorCategory.Indeterminate, LoadOutcome.Indeterminate<String, String>("1", error).category)
        assertEquals(ErrorCategory.Conflict, LoadOutcome.Conflict<String, String>("1", error).category)
        val loaded = LoadOutcome.Loaded("1", "ENTITY")
        assertEquals("1", loaded.id)
        assertEquals("ENTITY", loaded.entity)
    }

    @Test
    fun `toPersistFailure maps every load failure to the same persist category`() {
        val error = s2error("ERR", "desc")
        val rejected = LoadOutcome.Rejected<String, String>("1", error).toPersistFailure("msg")
        val transient = LoadOutcome.Transient<String, String>("1", error).toPersistFailure("msg")
        val indeterminate = LoadOutcome.Indeterminate<String, String>("1", error).toPersistFailure("msg")
        val conflict = LoadOutcome.Conflict<String, String>("1", error).toPersistFailure("msg")

        assertEquals(PersistOutcome.Rejected<Nothing>("msg", error), rejected)
        assertEquals(PersistOutcome.Transient<Nothing>("msg", error), transient)
        assertEquals(PersistOutcome.Indeterminate<Nothing>("msg", error), indeterminate)
        assertEquals(PersistOutcome.Conflict<Nothing>("msg", error), conflict)
        listOf(rejected, transient, indeterminate, conflict).forEach {
            assertEquals("msg", it.msgId)
            assertSame(error, it.error)
        }
    }

    @Test
    fun `LoadedSlot Ready and Failed expose their command envelope`() {
        val envelope = "CMD".asEnvelopeWithType("Cmd")
        val ready = LoadedSlot.Ready(envelope, "ENTITY")
        assertSame(envelope, ready.cmd)
        assertEquals("ENTITY", ready.entity)

        val failure = PersistOutcome.Rejected<Nothing>("msg", s2error("ERR", "desc"))
        val failed = LoadedSlot.Failed<String, String>(envelope, failure)
        assertSame(envelope, failed.cmd)
        assertSame(failure, failed.failure)
    }
}
