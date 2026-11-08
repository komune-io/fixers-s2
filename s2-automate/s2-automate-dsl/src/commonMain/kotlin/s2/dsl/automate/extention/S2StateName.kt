package s2.dsl.automate.extention

/**
 * The name of an S2 state, whether [status] is the state itself or the string a JSON payload
 * carries; `null` for anything else.
 *
 * The parameter is [Any] because the declared type of a payload's `status` lies in Kotlin/JS.
 */
expect fun s2StateName(status: Any?): String?
