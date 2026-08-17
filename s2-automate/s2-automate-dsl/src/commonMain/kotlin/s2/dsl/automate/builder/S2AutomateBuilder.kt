package s2.dsl.automate.builder

import kotlin.js.JsExport
import kotlin.js.JsName
import s2.dsl.automate.Cmd
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2Transition
import s2.dsl.automate.toValue

class S2AutomateBuilder {
	lateinit var name: String
	var version: String? = null
	val transactions = mutableListOf<S2Transition>()

	inline fun <reified CMD: S2InitCommand> init(exec: S2InitTransitionBuilder.() -> Unit) {
		val builder = S2InitTransitionBuilder()
		builder.exec()
		S2Transition(
			to = builder.to.toValue(),
			role = builder.role.toValue(),
			action = CMD::class.toValue(),
			from = null,
			result = builder.evt?.toValue()
		).let(transactions::add)
	}

	inline fun <reified CMD: Cmd> transaction(exec: S2TransitionBuilder.() -> Unit) {
		val builder = S2TransitionBuilder()
		builder.exec()
		builder.from?.let(builder.froms::add)
		builder.froms.map { from ->
			S2Transition(
				from = from.toValue(),
				to = builder.to.toValue(),
				role = builder.role.toValue(),
				action = CMD::class.toValue(),
				result = builder.evt?.toValue(),
			).let(transactions::add)
		}
	}

	inline fun <reified CMD: Cmd> selfTransaction(exec: S2SelfTransitionBuilder.() -> Unit) {
		val builder = S2SelfTransitionBuilder()
		builder.exec()
		builder.states.map { state ->
			S2Transition(
				from = state.toValue(),
				to = state.toValue(),
				role = builder.role.toValue(),
				action = CMD::class.toValue(),
				result = builder.evt?.toValue()
			)
		}.forEach(transactions::add)
	}

	fun node(exec: S2NodeBuilder.() -> Unit) {
		transactions.addAll(nodeTransitions(exec))
	}
}

@JsExport
@JsName("s2")
fun s2(exec: S2AutomateBuilder.() -> Unit): S2Automate {
	val builder = S2AutomateBuilder()
	builder.exec()
	return S2Automate(
		name = builder.name,
		version = builder.version,
		transitions = builder.transactions.toTypedArray()
	)
}
