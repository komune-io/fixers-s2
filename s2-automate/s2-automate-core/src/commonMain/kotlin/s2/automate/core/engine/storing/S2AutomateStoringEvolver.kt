package s2.automate.core.engine.storing

import kotlinx.coroutines.flow.Flow
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2State
import s2.sourcing.dsl.Decide

interface S2AutomateStoringEvolver<STATE : S2State, ID, ENTITY : WithS2State<STATE>, EVENT: Evt> {

	suspend fun <COMMAND: S2InitCommand, EVENT_OUT: EVENT> createWithEventFlow(
		commands: Flow<COMMAND>,
		build: suspend (cmd: COMMAND) -> Pair<ENTITY, EVENT_OUT>
	): Flow<EVENT_OUT>

	fun <COMMAND: S2InitCommand, EVENT_OUT: EVENT> evolve(
		build: suspend (cmd: COMMAND) -> Pair<ENTITY, EVENT_OUT>
	): Decide<COMMAND, EVENT_OUT>

	suspend fun <COMMAND: S2Command<ID>, EVENT_OUT: EVENT> doTransitionFlow(
		commands: Flow<COMMAND>,
		exec: suspend (COMMAND, ENTITY) -> Pair<ENTITY, EVENT_OUT>
	): Flow<EVENT_OUT>

	fun <COMMAND : S2Command<ID>, EVENT_OUT : EVENT> evolve(
		fnc: suspend (COMMAND, ENTITY) -> Pair<ENTITY, EVENT_OUT>
	): Decide<COMMAND, EVENT_OUT>

}