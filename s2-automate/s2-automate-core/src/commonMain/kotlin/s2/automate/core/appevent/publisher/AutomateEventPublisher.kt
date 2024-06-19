package s2.automate.core.appevent.publisher

import s2.automate.core.appevent.AutomateInitTransitionEnded
import s2.automate.core.appevent.AutomateInitTransitionStarted
import s2.automate.core.appevent.AutomateSessionError
import s2.automate.core.appevent.AutomateSessionStarted
import s2.automate.core.appevent.AutomateSessionStopped
import s2.automate.core.appevent.AutomateStateEntered
import s2.automate.core.appevent.AutomateStateExited
import s2.automate.core.appevent.AutomateTransitionEnded
import s2.automate.core.appevent.AutomateTransitionError
import s2.automate.core.appevent.AutomateTransitionNotAccepted
import s2.automate.core.appevent.AutomateTransitionStarted
import s2.automate.core.appevent.listener.AutomateListener
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

class AutomateEventPublisher<STATE, ID, ENTITY, AUTOMATE>(
	private val publisher: AppEventPublisher
): AutomateListener<STATE, ID, ENTITY, AUTOMATE>
where STATE : S2State, ENTITY : WithS2State<STATE>, ENTITY : WithS2Id<ID> {

	override fun automateStateEntered(event: AutomateStateEntered) {
		publisher.publish(event)
	}

	override fun automateStateExited(event: AutomateStateExited) {
		publisher.publish(event)
	}

	override fun automateTransitionNotAccepted(event: AutomateTransitionNotAccepted) {
		publisher.publish(event)
	}

	override fun automateInitTransitionStarted(event: AutomateInitTransitionStarted) {
		publisher.publish(event)
	}

	override fun automateInitTransitionEnded(event: AutomateInitTransitionEnded<STATE, ENTITY>) {
		publisher.publish(event)
	}

	override fun automateTransitionStarted(event: AutomateTransitionStarted) {
		publisher.publish(event)
	}

	override fun automateTransitionEnded(event: AutomateTransitionEnded<STATE, ENTITY>) {
		publisher.publish(event)
	}

	override fun automateTransitionError(event: AutomateTransitionError) {
		publisher.publish(event)
	}

	override fun automateSessionStarted(event: AutomateSessionStarted<AUTOMATE>) {
		publisher.publish(event)
	}

	override fun automateSessionStopped(event: AutomateSessionStopped<AUTOMATE>) {
		publisher.publish(event)
	}

	override fun automateSessionError(event: AutomateSessionError) {
		publisher.publish(event)
	}
}
