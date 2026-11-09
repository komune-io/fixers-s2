package s2.dsl.automate

import kotlin.js.JsExport
import kotlin.js.JsName

@JsExport
interface WithId<ID : Any> {
	@JsName("id")
	val id: ID
}
