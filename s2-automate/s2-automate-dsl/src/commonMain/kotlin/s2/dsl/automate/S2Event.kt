package s2.dsl.automate

import kotlin.js.JsExport
import kotlin.js.JsName

@JsExport
@JsName("S2Event")
interface S2Event<out STATE : S2State, ID : Any> : Evt, WithId<ID> {
	override val id: ID
	val type: STATE
}

@JsExport
@JsName("S2EventSuccess")
class S2EventSuccess<out STATE : S2State, out COMMAND : Cmd, ID : Any>(
	@JsName("id")
	val id: ID,
	val type: COMMAND,
	@JsName("from")
	val from: STATE,
	@JsName("to")
	val to: STATE,
) : Evt

@JsExport
@JsName("S2EventError")
class S2EventError<out STATE : S2State, out COMMAND : Cmd, ID : Any>(
	@JsName("id")
	val id: ID,
	val type: COMMAND,
	@JsName("from")
	val from: STATE,
	@JsName("to")
	val to: STATE,
	val error: S2Error,
) : Evt
