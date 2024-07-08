package s2.dsl.automate

import kotlin.js.JsExport
import kotlin.js.JsName
import kotlinx.serialization.Serializable
import s2.dsl.automate.ssm.toSsm

@Serializable
@JsExport
@JsName("S2Automate")
class S2Automate(
	val name: String,
	val version: String?,
	val transitions: Array<S2Transition>,
):Automate {

	val withResultAsAction = this.transitions.all { it.result != null }

	override fun getAvailableTransitions(state: S2State): Array<S2Transition> {
		return transitions.filter { it.from.isSame(state) }.toTypedArray()
	}

	override fun isAvailableTransition(currentState: S2State, msg: Msg): Boolean {
		return getAvailableTransitions(currentState).any { it.action.isInstance(msg) }
	}

	override fun isAvailableInitTransition(command: Msg): Boolean {
		return transitions.any { it.from == null && it.action.isInstance(command) }
	}



	override fun isFinalState(state: S2State): Boolean {
		return getAvailableTransitions(state).isEmpty()
	}

	override fun isSameState(from: S2State?, to: S2State): Boolean {
		return from?.position == to.position
	}
	private fun S2TransitionValue.isInstance(msg: Msg): Boolean {
		val msgAction = msg::class.toValue()
		return msgAction.name == name
	}

	private fun S2StateValue?.isSame(to: S2State): Boolean {
		return this?.position == to.position
	}
}
