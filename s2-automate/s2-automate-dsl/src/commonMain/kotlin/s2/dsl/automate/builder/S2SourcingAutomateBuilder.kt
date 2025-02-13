package s2.dsl.automate.builder

import kotlin.js.JsExport
import kotlin.js.JsName
import s2.dsl.automate.Cmd
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2Transition
import s2.dsl.automate.toValue

class S2SourcingAutomateBuilder {
	lateinit var name: String
	val version: String? = null
	val transactions = mutableListOf<S2Transition>()

	inline fun <reified CMD: S2InitCommand, reified EVT: Evt> init(exec: S2InitTransitionBuilder.() -> Unit) {
		val builder = S2InitTransitionBuilder()
		builder.exec()
		S2Transition(
			to = builder.to.toValue(),
			role = builder.role.toValue(),
			action = CMD::class.toValue(),
			from = null,
			result = (builder.evt ?: EVT::class).toValue()
		).let(transactions::add)
	}

	inline fun <reified CMD: Cmd, reified EVT: Evt> transaction(exec: S2TransitionBuilder.() -> Unit) {
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

	inline fun <reified CMD: Cmd, reified EVT: Evt> selfTransaction(exec: S2SelfTransitionBuilder.() -> Unit) {
		val builder = S2SelfTransitionBuilder()
		builder.exec()
		builder.states.map { state ->
			S2Transition(
				from = state.toValue(),
				to = state.toValue(),
				role = builder.role.toValue(),
				action = CMD::class.toValue(),
				result = (builder.evt ?: EVT::class).toValue()
			)
		}.forEach(transactions::add)
	}

	fun node(exec: S2NodeBuilder.() -> Unit) {
		val builder = S2NodeBuilder()
		builder.exec()
		transactions.addAll(builder.transactions)
	}
}

@JsExport
@JsName("s2Sourcing")
fun s2Sourcing(exec: S2SourcingAutomateBuilder.() -> Unit): S2Automate {
	val builder = S2SourcingAutomateBuilder()
	builder.exec()
	return S2Automate(
		name = builder.name,
		version = builder.version,
		transitions = builder.transactions.toTypedArray(),
	)
}
