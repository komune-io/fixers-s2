package s2.automate.core.engine.storing

import kotlinx.coroutines.flow.Flow
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2State
import s2.sourcing.dsl.Decide

typealias S2EvolveInitFnc<COMMAND, ENTITY, EVENT_OUT> = suspend (cmd: COMMAND) -> Pair<ENTITY, EVENT_OUT>
typealias S2EvolveFnc<COMMAND, ENTITY, EVENT_OUT> = suspend (COMMAND, ENTITY) -> Pair<ENTITY, EVENT_OUT>

interface S2AutomateStoringEvolver<STATE : S2State, ID, ENTITY : WithS2State<STATE>, EVENT: Evt> {

	suspend fun <COMMAND: S2InitCommand, EVENT_OUT: EVENT> evolve(
		commands: Flow<COMMAND>,
		build:  S2EvolveInitFnc<COMMAND, ENTITY, EVENT_OUT>
	): Flow<EVENT_OUT>

	fun <COMMAND: S2InitCommand, EVENT_OUT: EVENT> evolve(
		build:  S2EvolveInitFnc<COMMAND, ENTITY, EVENT_OUT>
	): Decide<COMMAND, EVENT_OUT>

	suspend fun <COMMAND: S2Command<ID>, EVENT_OUT: EVENT> evolve(
		commands: Flow<COMMAND>,
		exec: S2EvolveFnc<COMMAND, ENTITY, EVENT_OUT>
	): Flow<EVENT_OUT>

	fun <COMMAND : S2Command<ID>, EVENT_OUT : EVENT> evolve(
		fnc: S2EvolveFnc<COMMAND, ENTITY, EVENT_OUT>
	): Decide<COMMAND, EVENT_OUT>

}
