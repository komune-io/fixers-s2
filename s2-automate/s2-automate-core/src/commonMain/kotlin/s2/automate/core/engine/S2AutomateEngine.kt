package s2.automate.core.engine

import f2.dsl.cqrs.envelope.Envelope
import f2.dsl.cqrs.enveloped.EnvelopedFlow
import kotlin.js.JsName
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2State

@JsName("S2AutomateEngine")
interface S2AutomateEngine<STATE, ENTITY, ID, EVENT> where
ENTITY : WithS2State<STATE>,
STATE : S2State {
	suspend fun <COMMAND: S2InitCommand, ENTITY_OUT: ENTITY, EVENT_OUT : EVENT> create(
		commands: EnvelopedFlow<COMMAND>,
		decide: suspend (cmd: Envelope<COMMAND>) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
	): EnvelopedFlow<EVENT_OUT>

	suspend fun <COMMAND: S2Command<ID>, ENTITY_OUT: ENTITY, EVENT_OUT : EVENT> doTransition(
		commands: EnvelopedFlow<COMMAND>,
		exec: suspend (Envelope<out COMMAND>, ENTITY) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
	): EnvelopedFlow<EVENT_OUT>
}
