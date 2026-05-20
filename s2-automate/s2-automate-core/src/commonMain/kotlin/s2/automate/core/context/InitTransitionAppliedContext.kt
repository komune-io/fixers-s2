package s2.automate.core.context

import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

class InitTransitionAppliedContext<STATE, ID, ENTITY, EVENT, AUTOMATE>(
	val automateContext: AutomateContext<AUTOMATE>,
	val msgId: String,
	val msg: S2InitCommand,
	val event: EVENT,
	val entity: ENTITY,
) where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID>
