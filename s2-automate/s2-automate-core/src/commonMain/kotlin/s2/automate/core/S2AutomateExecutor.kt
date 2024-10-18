package s2.automate.core

import kotlin.js.JsName
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2State

@JsName("S2AutomateExecutor")
interface S2AutomateExecutor<STATE, ENTITY, ID, EVENT> where
ENTITY : WithS2State<STATE>,
STATE : S2State {
	suspend fun <ENTITY_OUT: ENTITY, EVENT_OUT : EVENT> create(
		command: S2InitCommand, decide: suspend () -> Pair<ENTITY_OUT, EVENT_OUT>
	): Pair<ENTITY_OUT, EVENT_OUT>

	suspend fun <COMMAND : S2Command<ID>, ENTITY_OUT: ENTITY, EVENT_OUT : EVENT> doTransition(
		command: COMMAND,
		exec: suspend ENTITY.() -> Pair<ENTITY_OUT, EVENT_OUT>
	): Pair<ENTITY_OUT, EVENT_OUT>
}
