package s2.dsl.automate.extention

import s2.dsl.automate.Cmd
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2State


inline fun <reified C: Cmd> S2Automate.canExecuteTransitionAnd(
    model: WithS2State<out S2State>, hasAccess: () -> Boolean
): Boolean {
    return isAvailableTransition(model, C::class.simpleName!!) && hasAccess()
}

fun S2Automate.isAvailableTransition(state: WithS2State<out S2State>?, type: String): Boolean {
    return state != null && isAvailableTransition(
        currentState = state.s2State(),
        type = type
    )
}

fun S2Automate.isAvailableTransition(currentState: S2State, type: String): Boolean {
    return getAvailableTransitions(currentState).any { it.result?.name == type || it.action.name == type }
}

/**
 * Compares by name, so the state may be the string a JSON payload carries: in Kotlin/JS a
 * payload's `status` is that string whatever its declared type, and comparing it to a state
 * object throws.
 */
fun S2Automate.isAvailableTransitionByName(stateName: String?, type: String): Boolean {
    return stateName != null && transitions.any {
        it.from?.name == stateName && (it.result?.name == type || it.action.name == type)
    }
}

inline fun <reified C: Cmd> S2Automate.canExecuteTransitionByNameAnd(
    stateName: String?, hasAccess: () -> Boolean
): Boolean {
    return isAvailableTransitionByName(stateName, C::class.simpleName!!) && hasAccess()
}
