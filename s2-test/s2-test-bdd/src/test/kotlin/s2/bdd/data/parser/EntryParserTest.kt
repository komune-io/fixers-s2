package s2.bdd.data.parser

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import s2.bdd.exception.IllegalDataTableParamException
import s2.bdd.exception.NoParserFoundException
import s2.bdd.exception.NullDataTableParamException

class EntryParserTest {

    companion object {
        init {
            registerPrimitiveParsers()
        }
    }

    private val intParser = EntryParserDirectory.select(Int::class)

    @Test
    fun `singleOrNull should return parsed value when key is present`() {
        assertThat(intParser.singleOrNull(mapOf("age" to "42"), "age")).isEqualTo(42)
    }

    @Test
    fun `singleOrNull should return null when key is absent`() {
        assertThat(intParser.singleOrNull(emptyMap(), "age")).isNull()
    }

    @Test
    fun `singleOrNull should throw IllegalDataTableParamException on unparseable value`() {
        assertThatThrownBy { intParser.singleOrNull(mapOf("age" to "abc"), "age") }
            .isInstanceOf(IllegalDataTableParamException::class.java)
            .hasMessageContaining("age")
            .hasMessageContaining("Expected Int value")
    }

    @Test
    fun `single should return parsed value when key is present`() {
        assertThat(intParser.single(mapOf("age" to "7"), "age")).isEqualTo(7)
    }

    @Test
    fun `single should throw NullDataTableParamException when key is absent`() {
        assertThatThrownBy { intParser.single(emptyMap(), "age") }
            .isInstanceOf(NullDataTableParamException::class.java)
            .hasMessageContaining("age")
    }

    @Test
    fun `listOrNull should split and trim comma-separated values`() {
        assertThat(intParser.listOrNull(mapOf("ids" to "1, 2 ,3"), "ids"))
            .containsExactly(1, 2, 3)
    }

    @Test
    fun `listOrNull should return null when key is absent`() {
        assertThat(intParser.listOrNull(emptyMap(), "ids")).isNull()
    }

    @Test
    fun `listOrNull should throw IllegalDataTableParamException on unparseable element`() {
        assertThatThrownBy { intParser.listOrNull(mapOf("ids" to "1,x"), "ids") }
            .isInstanceOf(IllegalDataTableParamException::class.java)
    }

    @Test
    fun `list should return parsed values`() {
        assertThat(intParser.list(mapOf("ids" to "5,6"), "ids")).containsExactly(5, 6)
    }

    @Test
    fun `list should throw NullDataTableParamException when key is absent`() {
        assertThatThrownBy { intParser.list(emptyMap(), "ids") }
            .isInstanceOf(NullDataTableParamException::class.java)
    }

    @Test
    fun `directory select should throw NoParserFoundException for unregistered type`() {
        class Unregistered
        assertThatThrownBy { EntryParserDirectory.select(Unregistered::class) }
            .isInstanceOf(NoParserFoundException::class.java)
            .hasMessageContaining("Unregistered")
    }

    @Test
    fun `directory register should allow selecting the registered parser`() {
        data class Custom(val raw: String)
        val parser = EntryParser(Custom::class, "invalid custom") { Custom(it) }
        assertThat(EntryParserDirectory.select(Custom::class)).isSameAs(parser)
        assertThat(parser.single(mapOf("c" to "v"), "c")).isEqualTo(Custom("v"))
    }
}

internal fun registerPrimitiveParsers() {
    StringParser()
    IntParser()
    LongParser()
    FloatParser()
    DoubleParser()
    BooleanParser()
}
