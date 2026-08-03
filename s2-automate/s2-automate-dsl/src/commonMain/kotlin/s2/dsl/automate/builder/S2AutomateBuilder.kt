package s2.dsl.automate.builder

import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.reflect.KClass
import s2.dsl.automate.Cmd
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2Transition
import s2.dsl.automate.toValue

class S2AutomateBuilder {
	lateinit var name: String
	var version: String? = null
	val transactions = mutableListOf<S2Transition>()

	inline fun <reified CMD: S2InitCommand> init(noinline exec: S2InitTransitionBuilder.() -> Unit) {
		init(CMD::class, exec)
	}

	fun init(command: KClass<out S2InitCommand>, exec: S2InitTransitionBuilder.() -> Unit) {
		val builder = S2InitTransitionBuilder()
		builder.exec()
		S2Transition(
			to = builder.to.toValue(),
			role = builder.role.toValue(),
			action = command.toValue(),
			from = null,
			result = builder.evt?.toValue()
		).let(transactions::add)
	}

	inline fun <reified CMD: Cmd> transaction(noinline exec: S2TransitionBuilder.() -> Unit) {
		transaction(CMD::class, exec)
	}

	fun transaction(command: KClass<out Cmd>, exec: S2TransitionBuilder.() -> Unit) {
		val builder = S2TransitionBuilder()
		builder.exec()
		builder.from?.let(builder.froms::add)
		builder.froms.map { from ->
			S2Transition(
				from = from.toValue(),
				to = builder.to.toValue(),
				role = builder.role.toValue(),
				action = command.toValue(),
				result = builder.evt?.toValue(),
			).let(transactions::add)
		}
	}

	inline fun <reified CMD: Cmd> selfTransaction(noinline exec: S2SelfTransitionBuilder.() -> Unit) {
		selfTransaction(CMD::class, exec)
	}

	fun selfTransaction(command: KClass<out Cmd>, exec: S2SelfTransitionBuilder.() -> Unit) {
		val builder = S2SelfTransitionBuilder()
		builder.exec()
		builder.states.map { state ->
			S2Transition(
				from = state.toValue(),
				to = state.toValue(),
				role = builder.role.toValue(),
				action = command.toValue(),
				result = builder.evt?.toValue()
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
