package s2.automate.core.error

import s2.dsl.automate.S2Error

class AutomateException(
    val errors: List<S2Error>,
) : Exception(
	errors.toString()
)

fun List<S2Error>.toString() = map { it.description }.reduce { desc1, desc2 -> "$desc1, $desc2" }
