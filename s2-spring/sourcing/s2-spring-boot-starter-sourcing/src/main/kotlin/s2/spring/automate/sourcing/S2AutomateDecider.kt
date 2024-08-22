package s2.spring.automate.sourcing

import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2State

interface S2AutomateDecider<ENTITY : WithS2State<STATE>, STATE : S2State, EVENT: Any, ID> {

	suspend fun <EVENT_OUT: EVENT> init(
		command: S2InitCommand, buildEvent: suspend () -> EVENT_OUT
	): EVENT_OUT

	suspend fun <EVENT_OUT: EVENT> transition(
		command: S2Command<ID>, exec: suspend ENTITY.() -> EVENT_OUT
	): EVENT_OUT

	suspend fun replayHistory()

}
