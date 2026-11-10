package s2.bdd.data.parser

import kotlin.reflect.KClass
import s2.bdd.exception.NoParserFoundException

object EntryParserDirectory {
    private val parsers = mutableMapOf<KClass<*>, EntryParser<*>>()

    inline fun <reified T: Any> register(parser: EntryParser<T>) {
        register(parser, T::class)
    }

    fun <T: Any> register(parser: EntryParser<T>, output: KClass<T>) {
        parsers[output] = parser
    }

    fun <T: Any> select(output: KClass<T>): EntryParser<T> {
        val parser = parsers[output]
            ?: throw NoParserFoundException(output)
        // Each parser is registered under the class it outputs.
        @Suppress("UNCHECKED_CAST")
        return parser as EntryParser<T>
    }
}
