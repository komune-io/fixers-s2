package s2.dsl.automate

import kotlin.js.JsExport
import kotlin.js.JsName

@JsExport
@JsName("S2InitCommand")
interface S2InitCommand : Cmd

@JsExport
@JsName("S2Command")
interface S2Command<ID : Any> : Cmd, WithId<ID>
