package s2.automate.core.error

import s2.dsl.automate.S2Error
import s2.dsl.automate.s2error

@Suppress("FunctionName")
fun ERROR_UNKNOWN(e: Exception) =
	s2error("ERROR_UNKNOWN",
		"An unknown error has occurred.",
			cause = e
		)

@Suppress("FunctionName")
fun ERROR_INVALID_TRANSITION(state: String, command: String) =
	s2error("ERROR_INVALID_TRANSITION",
		"Not available transition from $state with command $command",
		mapOf("from" to state, "command" to command))

@Suppress("FunctionName")
fun ERROR_ENTITY_NOT_FOUND(id: String) =
	s2error("ERROR_ENTITY_NOT_FOUND", "Entity with id[$id] not found", mapOf("id" to id))

fun S2Error.asException() = AutomateException(listOf(this), this.cause)

fun S2Error.throwException() {
	throw AutomateException(listOf(this))
}
