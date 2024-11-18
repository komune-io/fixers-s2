package s2.automate.core.engine.storing

import s2.dsl.automate.Evt
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2State

interface S2AutomateStoringEvolverOld<STATE : S2State, ID, ENTITY : WithS2State<STATE>, EVENT: Evt> {

	suspend fun <EVENT_OUT: EVENT> createWithEvent(
		command: S2InitCommand,
		buildEvent: suspend ENTITY.() -> EVENT_OUT,
		buildEntity: suspend () -> ENTITY,
	): EVENT_OUT

	suspend fun <EVENT_OUT: EVENT> createWithEvent(
		command: S2InitCommand,
		build: suspend () -> Pair<ENTITY, EVENT_OUT>
	): EVENT_OUT

	suspend fun <EVENT_OUT: EVENT> doTransition(
		command: S2Command<ID>,
		exec: suspend ENTITY.() -> Pair<ENTITY, EVENT_OUT>
	): EVENT_OUT

}
