package s2.automate.core.error

import s2.dsl.automate.S2Error
import s2.dsl.automate.error

fun ERROR_INVALID_TRANSITION(state: String, command: String) =
	S2Error.error("ERROR_INVALID_TRANSITION", "Not available transition from ${state} with command ${command}", mapOf("from" to state, "command" to command))
fun ERROR_ENTITY_NOT_FOUND(id: String) =
	S2Error.error("ERROR_ENTITY_NOT_FOUND", "Entity with id[$id] not found", mapOf("id" to id))

fun S2Error.asException() = AutomateException(listOf(this))

fun S2Error.throwException() {
	throw AutomateException(listOf(this))
}