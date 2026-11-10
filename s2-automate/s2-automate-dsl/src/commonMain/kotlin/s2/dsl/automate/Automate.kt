package s2.dsl.automate

import kotlin.js.JsExport

@JsExport
interface Automate {
    fun getAvailableTransitions(state: S2State): Array<S2Transition>
    fun isAvailableTransition(currentState: S2State, msg: Msg): Boolean
    fun isAvailableInitTransition(command: Msg): Boolean
    fun isFinalState(state: S2State): Boolean
    fun isSameState(from: S2State?, to: S2State): Boolean
}
