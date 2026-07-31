package s2.bdd.data.parser

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import s2.bdd.exception.IllegalDataTableParamException

class PrimitiveParsersTest {

    companion object {
        init {
            registerPrimitiveParsers()
        }
    }

    private val entry = mapOf(
        "name" to "alice",
        "int" to "42",
        "long" to "9000000000",
        "float" to "1.5",
        "double" to "2.25",
        "bool" to "true"
    )

    @Test
    fun `string parser should return raw value`() {
        assertThat(entry.extract<String>("name")).isEqualTo("alice")
    }

    @Test
    fun `int parser should parse valid value and reject invalid`() {
        assertThat(entry.extract<Int>("int")).isEqualTo(42)
        assertThatThrownBy { entry.extract<Int>("name") }
            .isInstanceOf(IllegalDataTableParamException::class.java)
    }

    @Test
    fun `long parser should parse valid value and reject invalid`() {
        assertThat(entry.extract<Long>("long")).isEqualTo(9_000_000_000L)
        assertThatThrownBy { entry.extract<Long>("name") }
            .isInstanceOf(IllegalDataTableParamException::class.java)
    }

    @Test
    fun `float parser should parse valid value and reject invalid`() {
        assertThat(entry.extract<Float>("float")).isEqualTo(1.5f)
        assertThatThrownBy { entry.extract<Float>("name") }
            .isInstanceOf(IllegalDataTableParamException::class.java)
    }

    @Test
    fun `double parser should parse valid value and reject invalid`() {
        assertThat(entry.extract<Double>("double")).isEqualTo(2.25)
        assertThatThrownBy { entry.extract<Double>("name") }
            .isInstanceOf(IllegalDataTableParamException::class.java)
    }

    @Test
    fun `boolean parser should parse strict boolean and reject invalid`() {
        assertThat(entry.extract<Boolean>("bool")).isTrue()
        assertThatThrownBy { entry.extract<Boolean>("name") }
            .isInstanceOf(IllegalDataTableParamException::class.java)
    }
}
