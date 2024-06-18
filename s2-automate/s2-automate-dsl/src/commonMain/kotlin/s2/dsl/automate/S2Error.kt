package s2.dsl.automate

import kotlin.js.JsExport
import kotlin.js.JsName

@JsExport
@JsName("S2Error")
interface S2Error {
	val type: String
	val description: String
	val date: String
	val payload: Map<String, String>
	val cause: Throwable?
}

@JsExport
@JsName("S2ErrorBase")
class S2ErrorBase(
	override val type: String,
	override val description: String,
	override val date: String,
	override val payload: Map<String, String>,
	override val cause: Throwable? = null
) : S2Error {

	override fun toString(): String {
		return "S2ErrorBase(type='$type', description='$description', date='$date', payload=$payload)"
	}
}

fun s2error(
	code: String,
	description: String,
	payload: Map<String, String> = emptyMap(),
	cause: Throwable? = null
): S2ErrorBase {
	return S2ErrorBase(
		type = code,
		description = description,
		date = "",
		payload = payload,
		cause = cause
	)
}

fun s2warning(code: String, description: String, payload: Map<String, String>): S2ErrorBase {
	return S2ErrorBase(
		type = code,
		description = description,
		date = "",
		payload = payload
	)
}
