package s2.automate.core.error

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import s2.dsl.automate.s2error

class AutomateErrorTest {

    @Test
    fun `unknownError wraps the cause`() {
        val cause = IllegalStateException("boom")
        val error = unknownError(cause)
        assertEquals("ERROR_UNKNOWN", error.type)
        assertEquals("An unknown error has occurred.", error.description)
        assertSame(cause, error.cause)
    }

    @Test
    fun `invalidTransitionError carries state and command in payload`() {
        val error = invalidTransitionError("Created", "DoCmd")
        assertEquals("ERROR_INVALID_TRANSITION", error.type)
        assertEquals("Created", error.payload["from"])
        assertEquals("DoCmd", error.payload["command"])
        assertTrue(error.description.contains("Created"))
    }

    @Test
    fun `entityNotFoundError carries the id in payload`() {
        val error = entityNotFoundError("42")
        assertEquals("ERROR_ENTITY_NOT_FOUND", error.type)
        assertEquals("42", error.payload["id"])
        assertTrue(error.description.contains("42"))
    }

    @Test
    fun `persistLambdaThrowError uses the cause message`() {
        val error = persistLambdaThrowError(IllegalStateException("boom"))
        assertEquals("ERROR_PERSIST_LAMBDA_THROW", error.type)
        assertEquals("boom", error.description)
    }

    @Test
    fun `persistLambdaThrowError falls back to the cause class name`() {
        val error = persistLambdaThrowError(IllegalStateException())
        assertEquals("IllegalStateException", error.description)
    }

    @Test
    fun `persistLambdaThrowError falls back to unknown for anonymous causes`() {
        val anonymous = object : Exception() {}
        val error = persistLambdaThrowError(anonymous)
        assertEquals("unknown", error.description)
    }

    @Test
    fun `asException builds an AutomateException carrying the error`() {
        val cause = IllegalStateException("boom")
        val exception = s2error("CODE", "desc", cause = cause).asException()
        assertEquals(1, exception.errors.size)
        assertEquals("CODE", exception.errors.first().type)
        assertSame(cause, exception.cause)
    }

    @Test
    fun `throwException throws an AutomateException`() {
        val exception = assertThrows<AutomateException> {
            s2error("CODE", "desc").throwException()
        }
        assertEquals("CODE", exception.errors.single().type)
        assertNull(exception.cause)
    }
}
