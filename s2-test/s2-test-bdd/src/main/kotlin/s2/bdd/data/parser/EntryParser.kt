package s2.bdd.data.parser

import kotlin.reflect.KClass
import s2.bdd.exception.IllegalDataTableParamException
import s2.bdd.exception.NullDataTableParamException

open class EntryParser<R: Any>(
    output: KClass<R>,
    protected val parseErrorMessage: String,
    protected val parser: (String) -> R?
) {
    init {
        EntryParserDirectory.register(this, output)
    }

    fun singleOrNull(entry: Map<String, String>, key: String): R? {
        return entry[key]?.let {
            parser(it) ?: throw IllegalDataTableParamException(key, parseErrorMessage)
        }
    }

    fun single(entry: Map<String, String>, key: String): R {
        return singleOrNull(entry, key)
            ?: throw NullDataTableParamException(key)
    }

    fun listOrNull(entry: Map<String, String>, key: String): List<R>? {
        return entry[key]
            ?.split(",")
            ?.map {
                parser(it.trim()) ?: throw IllegalDataTableParamException(key, parseErrorMessage)
            }
    }

    fun list(entry: Map<String, String>, key: String): List<R> {
        return listOrNull(entry, key)
            ?: throw NullDataTableParamException(key)
    }
}
