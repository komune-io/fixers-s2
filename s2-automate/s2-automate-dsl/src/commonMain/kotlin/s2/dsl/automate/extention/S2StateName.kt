package s2.dsl.automate.extention

import kotlin.js.JsExport
import s2.dsl.automate.S2State
import s2.dsl.automate.s2Name

/**
 * The name of an S2 state, whether [status] is the state itself or the string a JSON payload
 * carries; `null` for anything else.
 *
 * The parameter is [Any] because the declared type of a payload's `status` lies in Kotlin/JS.
 */
@JsExport
fun s2StateName(status: Any?): String? = when (status) {
    null -> null
    is String -> status
    is S2State -> status.s2Name()
    else -> enumCopyName(status)
}

/** The name of a JSON copy of a Kotlin/JS enum; `null` where no such copy exists. */
internal expect fun enumCopyName(value: Any): String?
