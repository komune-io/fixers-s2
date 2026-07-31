@file:Suppress("DEPRECATION")

package s2.bdd.data.parser

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import s2.bdd.exception.IllegalDataTableParamException
import s2.bdd.exception.NullDataTableParamException

class GenericParserTest {

    companion object {
        init {
            registerPrimitiveParsers()
        }
    }

    private val entry = mapOf("count" to "3", "ids" to "1,2", "raw" to "abc")

    @Test
    fun `deprecated extract should parse value with provided parser`() {
        assertThat(entry.extract<Int>("count", "Expected Int") { it.toIntOrNull() }).isEqualTo(3)
        assertThat(entry.extract<Int>("missing", "Expected Int") { it.toIntOrNull() }).isNull()
    }

    @Test
    fun `deprecated extract should throw on unparseable value`() {
        assertThatThrownBy { entry.extract<Int>("raw", "Expected Int") { it.toIntOrNull() } }
            .isInstanceOf(IllegalDataTableParamException::class.java)
            .hasMessageContaining("Expected Int")
    }

    @Test
    fun `deprecated safeExtract should throw when key is absent`() {
        assertThat(entry.safeExtract<Int>("count", "Expected Int") { it.toIntOrNull() }).isEqualTo(3)
        assertThatThrownBy { entry.safeExtract<Int>("missing", "Expected Int") { it.toIntOrNull() } }
            .isInstanceOf(NullDataTableParamException::class.java)
    }

    @Test
    fun `deprecated extractList should parse each element`() {
        assertThat(entry.extractList<Int>("ids", "Expected Int") { it.toIntOrNull() })
            .containsExactly(1, 2)
        assertThat(entry.extractList<Int>("missing", "Expected Int") { it.toIntOrNull() }).isNull()
    }

    @Test
    fun `deprecated safeExtractList should throw when key is absent`() {
        assertThat(entry.safeExtractList<Int>("ids", "Expected Int") { it.toIntOrNull() })
            .containsExactly(1, 2)
        assertThatThrownBy { entry.safeExtractList<Int>("missing", "Expected Int") { it.toIntOrNull() } }
            .isInstanceOf(NullDataTableParamException::class.java)
    }
}
