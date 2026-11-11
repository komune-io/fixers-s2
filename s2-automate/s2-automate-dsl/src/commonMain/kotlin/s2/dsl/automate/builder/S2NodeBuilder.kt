package s2.dsl.automate.builder

import kotlin.properties.Delegates
import s2.dsl.automate.Cmd
import s2.dsl.automate.S2State
import s2.dsl.automate.S2Transition
import s2.dsl.automate.toValue

internal fun nodeTransitions(exec: S2NodeBuilder.() -> Unit): List<S2Transition> =
    S2NodeBuilder().apply(exec).transactions

class S2NodeBuilder {
    var state: S2State by Delegates.notNull()
    val transactions: MutableList<S2Transition> = mutableListOf()

    inline fun <reified CMD : Cmd> transaction(
        exec: S2NodeTransitionBuilder.() -> Unit,
    ) {
        val builder = S2NodeTransitionBuilder()
        builder.exec()
        transactions += S2Transition(
            from = state.toValue(),
            to = (builder.to ?: state).toValue(),
            role = builder.role.toValue(),
            action = CMD::class.toValue(),
            result = builder.evt?.toValue()
        )
    }
}
