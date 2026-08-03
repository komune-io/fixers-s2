package s2.dsl.automate.builder

import kotlin.reflect.KClass
import s2.dsl.automate.Cmd
import s2.dsl.automate.S2State
import s2.dsl.automate.S2Transition
import s2.dsl.automate.toValue

class S2NodeBuilder {
    lateinit var state: S2State
    val transactions: MutableList<S2Transition> = mutableListOf()

    inline fun <reified CMD : Cmd> transaction(noinline exec: S2NodeTransitionBuilder.() -> Unit) {
        transaction(CMD::class, exec)
    }

    fun transaction(command: KClass<out Cmd>, exec: S2NodeTransitionBuilder.() -> Unit) {
        val builder = S2NodeTransitionBuilder()
        builder.exec()
        S2Transition(
            from = state.toValue(),
            to = (builder.to ?: state).toValue(),
            role = builder.role.toValue(),
            action = command.toValue(),
            result = builder.evt?.toValue()
        ).let(transactions::add)
    }
}
