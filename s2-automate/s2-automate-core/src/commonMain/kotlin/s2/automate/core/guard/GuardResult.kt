package s2.automate.core.guard

import s2.dsl.automate.S2Error

data class GuardResult(
    val errors: List<S2Error>,
) {
    companion object {
        fun valid(): GuardResult = GuardResult(emptyList())
        fun error(vararg errors: S2Error): GuardResult = GuardResult(errors.asList())
        fun error(errors: List<S2Error>): GuardResult = GuardResult(errors)
    }
}

fun GuardResult.isValid(): Boolean = errors.isEmpty()
