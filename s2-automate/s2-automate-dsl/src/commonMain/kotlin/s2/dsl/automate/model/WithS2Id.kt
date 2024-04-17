package s2.dsl.automate.model

import kotlin.js.JsExport

@JsExport
interface WithS2Id<ID> {
	fun s2Id(): ID & Any
}

