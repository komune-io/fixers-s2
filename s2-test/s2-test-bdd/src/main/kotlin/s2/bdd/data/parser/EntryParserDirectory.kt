package s2.bdd.data.parser

import s2.bdd.exception.NoParserFoundException
import kotlin.reflect.KClass

object EntryParserDirectory {
    private val parsers = mutableMapOf<KClass<*>, EntryParser<*>>()

    inline fun <reified T: Any> register(parser: EntryParser<T>) {
        register(parser, T::class)
    }

    fun <T: Any> register(parser: EntryParser<T>, output: KClass<T>) {
        parsers[output] = parser
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> select(output: KClass<T>): EntryParser<T> {
        val parser = parsers[output]
            ?: throw NoParserFoundException(output)
        return parser as EntryParser<T>
    }
}
