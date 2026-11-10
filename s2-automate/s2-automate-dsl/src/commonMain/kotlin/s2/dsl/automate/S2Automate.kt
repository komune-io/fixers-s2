package s2.dsl.automate

import kotlin.js.JsExport
import kotlin.js.JsName
import kotlinx.serialization.Serializable

/**
 * A state is identified by its position, which the engine compares, and by its name, which a JSON
 * payload carries. The two must agree, so two states sharing either one are refused: an automate
 * serialized when names were the enum's class name fails here instead of refusing every state.
 */
@Serializable
@JsExport
@JsName("S2Automate")
class S2Automate(
    val name: String,
    val version: String?,
    val transitions: Array<S2Transition>,
):Automate {

    // The name is the wire contract: the JSON Jackson writes and JS callers read this property by it.
    @Suppress("BooleanPropertyNaming")
    val withResultAsAction = this.transitions.all { it.result != null }

    init {
        val states = transitions.flatMap { listOfNotNull(it.from, it.to) }
        requireOneToOne(states = states, key = { it.name }, value = { it.position }, label = "name")
        requireOneToOne(states = states, key = { it.position }, value = { it.name }, label = "position")
    }

    override fun getAvailableTransitions(state: S2State): Array<S2Transition> =
        transitions.filter { it.from.isSame(state) }.toTypedArray()

    override fun isAvailableTransition(currentState: S2State, msg: Msg): Boolean =
        getAvailableTransitions(currentState).any { it.action.isInstance(msg) }

    override fun isAvailableInitTransition(command: Msg): Boolean =
        transitions.any { it.from == null && it.action.isInstance(command) }

    override fun isFinalState(state: S2State): Boolean = getAvailableTransitions(state).isEmpty()

    override fun isSameState(from: S2State?, to: S2State): Boolean = from?.position == to.position

    private fun S2TransitionValue.isInstance(msg: Msg): Boolean {
        val msgAction = msg::class.toValue()
        return msgAction.name == name
    }

    private fun S2StateValue?.isSame(to: S2State): Boolean = this?.position == to.position
}

private fun <K : Any, V : Any> requireOneToOne(
    states: List<S2StateValue>, key: (S2StateValue) -> K, value: (S2StateValue) -> V, label: String,
) {
    states.groupBy(key).forEach { (k, sharing) ->
        val values = sharing.map(value).distinct()
        require(values.size == 1) { "States sharing the $label $k: $values" }
    }
}
