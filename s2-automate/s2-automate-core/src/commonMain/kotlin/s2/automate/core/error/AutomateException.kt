package s2.automate.core.error

import s2.dsl.automate.S2Error

class AutomateException(
    val errors: List<S2Error>,
    cause: Throwable? = null
) : Exception(
	errors.toString(),
    cause
)
