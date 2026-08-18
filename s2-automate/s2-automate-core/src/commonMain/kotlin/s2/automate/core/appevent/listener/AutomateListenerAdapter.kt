package s2.automate.core.appevent.listener

import s2.automate.core.appevent.AutomateInitTransitionEnded
import s2.automate.core.appevent.AutomateInitTransitionStarted
import s2.automate.core.persist.AutomatePersistFailure
import s2.automate.core.appevent.AutomateSessionStarted
import s2.automate.core.appevent.AutomateSessionStopped
import s2.automate.core.appevent.AutomateStateEntered
import s2.automate.core.appevent.AutomateStateExited
import s2.automate.core.appevent.AutomateTransitionEnded
import s2.automate.core.appevent.AutomateTransitionError
import s2.automate.core.appevent.AutomateTransitionNotAccepted
import s2.automate.core.appevent.AutomateTransitionStarted
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

open class AutomateListenerAdapter<STATE, ID, ENTITY, AUTOMATE> : AutomateListener<STATE, ID, ENTITY, AUTOMATE>
		where STATE : S2State, ENTITY : WithS2State<STATE>, ENTITY : WithS2Id<ID> {

	override fun automateStateEntered(event: AutomateStateEntered) { /* no-op by default, override to react to this event */ }

	override fun automateStateExited(event: AutomateStateExited) { /* no-op by default, override to react to this event */ }

	override fun automateTransitionNotAccepted(event: AutomateTransitionNotAccepted) { /* no-op by default, override to react to this event */ }

	override fun automateInitTransitionStarted(event: AutomateInitTransitionStarted) { /* no-op by default, override to react to this event */ }

	override fun automateInitTransitionEnded(event: AutomateInitTransitionEnded<STATE, ENTITY>) { /* no-op by default, override to react to this event */ }

	override fun automateTransitionStarted(event: AutomateTransitionStarted) { /* no-op by default, override to react to this event */ }

	override fun automateTransitionEnded(event: AutomateTransitionEnded<STATE, ENTITY>) { /* no-op by default, override to react to this event */ }

	override fun automateTransitionError(event: AutomateTransitionError) { /* no-op by default, override to react to this event */ }

	override fun automateSessionStarted(event: AutomateSessionStarted<AUTOMATE>) { /* no-op by default, override to react to this event */ }

	override fun automateSessionStopped(event: AutomateSessionStopped<AUTOMATE>) { /* no-op by default, override to react to this event */ }

	override fun automatePersistFailure(event: AutomatePersistFailure) { /* no-op by default, override to react to this event */ }
}
