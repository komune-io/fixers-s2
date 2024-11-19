package s2.automate.core.sourcing

import kotlinx.coroutines.flow.Flow
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2State
import s2.sourcing.dsl.Decide

interface S2AutomateSourcingDeciderFlow<ENTITY : WithS2State<STATE>, STATE : S2State, EVENT: Evt, ID> {

	suspend fun <COMMAND: S2InitCommand, EVENT_OUT: EVENT> decide(
		commands: Flow<COMMAND>,
		buildEvent: suspend (cmd: COMMAND) -> EVENT_OUT
	): Flow<EVENT_OUT>

	suspend fun <COMMAND: S2Command<ID>, EVENT_OUT: EVENT> decide(
		commands: Flow<COMMAND>,
		exec: suspend (COMMAND, ENTITY) -> EVENT_OUT
	): Flow<EVENT_OUT>

	suspend fun replayHistory()


	fun <EVENT_OUT : EVENT, COMMAND : S2InitCommand> decide(
		fnc: suspend (t: COMMAND) -> EVENT_OUT
	): Decide<COMMAND, EVENT_OUT>

	fun <COMMAND : S2Command<ID>, EVENT_OUT : EVENT> decide(
		fnc: suspend (t: COMMAND, entity: ENTITY) -> EVENT_OUT
	): Decide<COMMAND, EVENT_OUT>
}
