package s2.automate.core.appevent.listener

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
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

interface AutomateListener<STATE, ID, ENTITY, AUTOMATE>
		where STATE : S2State, ENTITY : WithS2State<STATE>, ENTITY : WithS2Id<ID> {

	/**
	 * Notified when state is entered.
	 *
	 * @param state the state
	 */
	fun automateStateEntered(event: AutomateStateEntered)

	/**
	 * Notified when state is exited.
	 *
	 * @param state the state
	 */
	fun automateStateExited(event: AutomateStateExited)

	/**
	 * Notified when event was not accepted.
	 *
	 * @param event the event
	 */
	fun automateTransitionNotAccepted(event: AutomateTransitionNotAccepted)

	/**
	 * Notified when init transition started.
	 *
	 * @param transition the transition
	 */
	fun automateInitTransitionStarted(event: AutomateInitTransitionStarted)

	/**
	 * Notified when transition ended.
	 *
	 * @param transition the transition
	 */
	fun automateInitTransitionEnded(event: AutomateInitTransitionEnded<STATE, ENTITY>)

	/**
	 * Notified when transition started.
	 *
	 * @param transition the transition
	 */
	fun automateTransitionStarted(event: AutomateTransitionStarted)

	/**
	 * Notified when transition ended.
	 *
	 * @param transition the transition
	 */
	fun automateTransitionEnded(event: AutomateTransitionEnded<STATE, ENTITY>)

	/**
	 * Notified when transition happened.
	 *
	 * @param transition the transition
	 */
	fun automateTransitionError(event: AutomateTransitionError)

	/**
	 * Notified when statemachine starts
	 *
	 * @param stateMachine the statemachine
	 */
	fun automateSessionStarted(event: AutomateSessionStarted<AUTOMATE>)

	/**
	 * Notified when statemachine stops
	 *
	 * @param stateMachine the statemachine
	 */
	fun automateSessionStopped(event: AutomateSessionStopped<AUTOMATE>)

	/**
	 * Notified when statemachine enters error it can't recover from.
	 *
	 * @param stateMachine the state machine
	 * @param exception the exception
	 */
	fun automateSessionError(event: AutomateSessionError)
}
