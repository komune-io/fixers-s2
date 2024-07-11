@file:Suppress("DEPRECATION")
package s2.bdd.data.parser

import s2.bdd.exception.IllegalDataTableParamException
import s2.bdd.exception.NullDataTableParamException

@Deprecated("Define a new EntryParser instead")
fun <R: Any> Map<String, String>.extract(
    key: String, parseErrorMessage: String, parser: (String) -> R?
) = get(key)?.let {
    parser(it) ?: throw IllegalDataTableParamException(key, parseErrorMessage)
}

@Deprecated("Define a new EntryParser instead")
fun <R: Any> Map<String, String>.safeExtract(
    key: String, parseErrorMessage: String, parser: (String) -> R?
) = extract(key, parseErrorMessage, parser) ?: throw NullDataTableParamException(key)

@Deprecated("Define a new EntryParser instead")
fun <R: Any> Map<String, String>.extractList(
    key: String, parseErrorMessage: String, parser: (String) -> R?
) = extractList<String>(key)?.map {
    parser(it) ?: throw IllegalDataTableParamException(key, parseErrorMessage)
}
@Deprecated("Define a new EntryParser instead")
fun <R: Any> Map<String, String>.safeExtractList(
    key: String, parseErrorMessage: String, parser: (String) -> R?
) = extractList(key, parseErrorMessage, parser) ?: throw NullDataTableParamException(key)
