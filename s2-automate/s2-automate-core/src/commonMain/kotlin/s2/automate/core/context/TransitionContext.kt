package s2.automate.core.context

import s2.dsl.automate.Cmd
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

class TransitionContext<STATE, ID, ENTITY, AUTOMATE, COMMAND>(
	val automateContext: AutomateContext<AUTOMATE>,
	val from: STATE,
	val command: COMMAND,
	val entity: ENTITY,
) where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID>,
COMMAND : Cmd
