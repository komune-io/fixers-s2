package s2.automate.core

import kotlin.js.JsName
import kotlinx.coroutines.flow.Flow
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2State

@JsName("S2AutomateExecutorFlow")
interface S2AutomateExecutorFlow<STATE, ENTITY, ID, EVENT> where
ENTITY : WithS2State<STATE>,
STATE : S2State {

	suspend fun <COMMAND: S2InitCommand, ENTITY_OUT: ENTITY, EVENT_OUT : EVENT> createInitFlow(
		commands: Flow<COMMAND>,
		decide: suspend (cmd: COMMAND) -> Pair<ENTITY_OUT, EVENT_OUT>
	): Flow<EVENT_OUT>

	suspend fun <COMMAND:S2Command<ID>, ENTITY_OUT: ENTITY, EVENT_OUT : EVENT> doTransitionFlow(
		commands: Flow<COMMAND>,
		exec: suspend (COMMAND, ENTITY) -> Pair<ENTITY_OUT, EVENT_OUT>
	): Flow<EVENT_OUT>
}
