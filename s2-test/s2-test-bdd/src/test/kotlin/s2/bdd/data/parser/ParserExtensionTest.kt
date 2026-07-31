package s2.bdd.data.parser

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import s2.bdd.exception.NullDataTableParamException

class ParserExtensionTest {

    companion object {
        init {
            registerPrimitiveParsers()
        }
    }

    private val entry = mapOf("count" to "3", "ids" to "1,2,3")

    @Test
    fun `extract should return parsed value or null`() {
        assertThat(entry.extract<Int>("count")).isEqualTo(3)
        assertThat(entry.extract<Int>("missing")).isNull()
    }

    @Test
    fun `safeExtract should return parsed value or throw`() {
        assertThat(entry.safeExtract<Int>("count")).isEqualTo(3)
        assertThatThrownBy { entry.safeExtract<Int>("missing") }
            .isInstanceOf(NullDataTableParamException::class.java)
    }

    @Test
    fun `extractList should return parsed list or null`() {
        assertThat(entry.extractList<Int>("ids")).containsExactly(1, 2, 3)
        assertThat(entry.extractList<Int>("missing")).isNull()
    }

    @Test
    fun `safeExtractList should return parsed list or throw`() {
        assertThat(entry.safeExtractList<Int>("ids")).containsExactly(1, 2, 3)
        assertThatThrownBy { entry.safeExtractList<Int>("missing") }
            .isInstanceOf(NullDataTableParamException::class.java)
    }
}
