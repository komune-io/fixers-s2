package s2.dsl.automate.model

import kotlin.js.JsExport

@JsExport
fun interface WithS2Id<ID : Any> {
    fun s2Id(): ID
}

