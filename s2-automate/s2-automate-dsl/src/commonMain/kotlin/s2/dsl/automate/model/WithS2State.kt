package s2.dsl.automate.model

import kotlin.js.JsExport

@JsExport
fun interface WithS2State<STATE> {
	fun s2State(): STATE
}
