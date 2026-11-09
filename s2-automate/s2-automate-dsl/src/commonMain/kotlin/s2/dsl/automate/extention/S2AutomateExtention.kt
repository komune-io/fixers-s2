package s2.dsl.automate.extention

import s2.dsl.automate.Cmd
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2State
import s2.dsl.automate.isNamed
import s2.dsl.automate.model.WithS2State


inline fun <reified C: Cmd> S2Automate.canExecuteTransitionAnd(
    model: WithS2State<out S2State>, hasAccess: () -> Boolean
): Boolean {
    return isAvailableTransition(model, requireNotNull(C::class.simpleName)) && hasAccess()
}

fun S2Automate.isAvailableTransition(state: WithS2State<out S2State>?, type: String): Boolean {
    return state != null && isAvailableTransition(
        currentState = state.s2State(),
        type = type
    )
}

fun S2Automate.isAvailableTransition(currentState: S2State, type: String): Boolean {
    return getAvailableTransitions(currentState).any { it.isNamed(type) }
}

/**
 * Compares by name, so the state may be the string a JSON payload carries: in Kotlin/JS a
 * payload's `status` is that string whatever its declared type, and comparing it to a state
 * object throws. [S2Automate] refuses two states sharing a name or a position, so this answers
 * as [isAvailableTransition] does for the same state.
 */
fun S2Automate.isAvailableTransitionByName(stateName: String?, type: String): Boolean {
    return stateName != null && transitions.any {
        it.from?.name == stateName && it.isNamed(type)
    }
}

/**
 * [canExecuteTransitionAnd] for a state known by its name, such as [s2StateName] of a JSON payload's
 * `status`: [hasAccess] is asked only when [C] leaves that state.
 */
inline fun <reified C: Cmd> S2Automate.canExecuteTransitionByNameAnd(
    stateName: String?, hasAccess: () -> Boolean
): Boolean {
    return isAvailableTransitionByName(stateName, requireNotNull(C::class.simpleName)) && hasAccess()
}
