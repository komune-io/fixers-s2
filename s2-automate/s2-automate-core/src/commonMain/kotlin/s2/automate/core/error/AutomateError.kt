package s2.automate.core.error

import s2.dsl.automate.S2Error
import s2.dsl.automate.s2error

fun unknownError(e: Exception) =
	s2error("ERROR_UNKNOWN",
		"An unknown error has occurred.",
			cause = e
		)

fun invalidTransitionError(state: String, command: String) =
	s2error("ERROR_INVALID_TRANSITION",
		"Not available transition from $state with command $command",
		mapOf("from" to state, "command" to command))

fun entityNotFoundError(id: String) =
	s2error("ERROR_ENTITY_NOT_FOUND", "Entity with id[$id] not found", mapOf("id" to id))

fun persisterEventCountError(expected: Int, actual: Int) =
	s2error(
		code = "ERROR_PERSISTER_EVENT_COUNT",
		description = "Persister contract violation: expected $expected persisted event(s) " +
			"for the chunk but received $actual. AutomatePersister.persist must emit exactly " +
			"one event per received context, in the same order.",
		payload = mapOf("expected" to expected.toString(), "actual" to actual.toString()),
	)

fun persistLambdaThrowError(cause: Throwable) =
    s2error(
        code = "ERROR_PERSIST_LAMBDA_THROW",
        description = cause.message ?: cause::class.simpleName ?: "unknown",
        cause = cause,
    )

@Deprecated("Use unknownError", ReplaceWith("unknownError(e)"))
@Suppress("FunctionNaming", "kotlin:S100")
fun ERROR_UNKNOWN(e: Exception) = unknownError(e)

@Deprecated("Use invalidTransitionError", ReplaceWith("invalidTransitionError(state, command)"))
@Suppress("FunctionNaming", "kotlin:S100")
fun ERROR_INVALID_TRANSITION(state: String, command: String) = invalidTransitionError(state, command)

@Deprecated("Use entityNotFoundError", ReplaceWith("entityNotFoundError(id)"))
@Suppress("FunctionNaming", "kotlin:S100")
fun ERROR_ENTITY_NOT_FOUND(id: String) = entityNotFoundError(id)

@Deprecated("Use persistLambdaThrowError", ReplaceWith("persistLambdaThrowError(cause)"))
@Suppress("FunctionNaming", "kotlin:S100")
fun ERROR_PERSIST_LAMBDA_THROW(cause: Throwable) = persistLambdaThrowError(cause)

fun S2Error.asException() = AutomateException(listOf(this), this.cause)

fun S2Error.throwException() {
	throw AutomateException(listOf(this))
}
