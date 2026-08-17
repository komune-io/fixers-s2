package s2.dsl.automate.builder

import s2.dsl.automate.Cmd
import s2.dsl.automate.S2State
import s2.dsl.automate.S2Transition
import s2.dsl.automate.toValue

/**
 * Builds the transitions declared by a `node { }` block. Shared by
 * [S2AutomateBuilder.node] and [S2SourcingAutomateBuilder.node].
 */
internal fun nodeTransitions(exec: S2NodeBuilder.() -> Unit): List<S2Transition> =
    S2NodeBuilder().apply(exec).transactions

class S2NodeBuilder {
    lateinit var state: S2State
    val transactions: MutableList<S2Transition> = mutableListOf()

    inline fun <reified CMD : Cmd> transaction(
        exec: S2NodeTransitionBuilder.() -> Unit,
    ) {
        val builder = S2NodeTransitionBuilder()
        builder.exec()
        S2Transition(
            from = state.toValue(),
            to = (builder.to ?: state).toValue(),
            role = builder.role.toValue(),
            action = CMD::class.toValue(),
            result = builder.evt?.toValue()
        ).let(transactions::add)
    }
}
