package s2.automate.core.appevent.publisher

import s2.automate.core.appevent.*
import s2.automate.core.appevent.listener.AutomateListener
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

class AutomateAppEventPublisher<STATE, ID, ENTITY>(
	private val publisher: AppEventPublisher
): AutomateListener<STATE, ID, ENTITY>
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

	override fun automateInitTransitionEnded(event: AutomateInitTransitionEnded<STATE, ID, ENTITY>) {
		publisher.publish(event)
	}

	override fun automateTransitionStarted(event: AutomateTransitionStarted) {
		publisher.publish(event)
	}

	override fun automateTransitionEnded(event: AutomateTransitionEnded<STATE, ID, ENTITY>) {
		publisher.publish(event)
	}

	override fun automateTransitionError(event: AutomateTransitionError) {
		publisher.publish(event)
	}

	override fun automateSessionStarted(event: AutomateSessionStarted) {
		publisher.publish(event)
	}

	override fun automateSessionStopped(event: AutomateSessionStopped) {
		publisher.publish(event)
	}

	override fun automateSessionError(event: AutomateSessionError) {
		publisher.publish(event)
	}

}