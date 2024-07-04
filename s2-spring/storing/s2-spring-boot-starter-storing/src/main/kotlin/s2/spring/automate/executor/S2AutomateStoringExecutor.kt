package s2.spring.automate.executor

import kotlinx.coroutines.flow.Flow
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2State

interface S2AutomateStoringExecutor<STATE : S2State, ID, ENTITY : WithS2State<STATE>, EVENT: Evt> {

	suspend fun <EVENT_OUT: EVENT> createWithEvent(
		command: S2InitCommand,
		buildEvent: suspend ENTITY.() -> EVENT_OUT,
		buildEntity: suspend () -> ENTITY,
	): EVENT_OUT

	suspend fun <EVENT_OUT: EVENT> createWithEvent(
		command: S2InitCommand, build: suspend () -> Pair<ENTITY, EVENT_OUT>
	): EVENT_OUT

	@Deprecated("Use doTransition with S2Command<ID>")
	suspend fun <EVENT_OUT: EVENT> doTransition(
		id: ID, command: S2Command<ID>,
		exec: suspend ENTITY.() -> Pair<ENTITY, EVENT_OUT>
	): EVENT_OUT

	suspend fun <EVENT_OUT: EVENT> doTransition(
		command: S2Command<ID>, exec: suspend ENTITY.() -> Pair<ENTITY, EVENT_OUT>
	): EVENT_OUT

	suspend fun <COMMAND: S2InitCommand, EVENT_OUT: EVENT> createWithEventFlow(
		command: Flow<COMMAND>,
		build: suspend (cmd: COMMAND) -> Pair<ENTITY, EVENT_OUT>
	): Flow<EVENT_OUT>

	suspend fun <COMMAND: S2Command<ID>, EVENT_OUT: EVENT> doTransitionFlow(
		command: Flow<COMMAND>,
		exec: suspend (COMMAND, ENTITY) -> Pair<ENTITY, EVENT_OUT>
	): Flow<EVENT_OUT>

}
