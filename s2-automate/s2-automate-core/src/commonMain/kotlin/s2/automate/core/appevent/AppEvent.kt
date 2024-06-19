package s2.automate.core.appevent

import s2.dsl.automate.Msg
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2State

interface AppEvent

/**
 * Notification when state is entered.
 *
 * @param state the state
 */
class AutomateStateEntered(
	val state: S2State,
) : AppEvent

/**
 * Notification when state is exited.
 *
 * @param state the state
 */
class AutomateStateExited(
	val state: S2State,
) : AppEvent

/**
 * Notification when command was not accepted.
 *
 * @param event the event
 */
class AutomateTransitionNotAccepted(
	val from: S2State?,
	val msg: Msg,
) : AppEvent

/**
 * Notification when init transition started.
 *
 * @param transition the transition
 */
class AutomateInitTransitionStarted(
	val msg: Msg,
) : AppEvent

/**
 * Notification when init transition ended.
 *
 * @param transition the transition
 */
class AutomateInitTransitionEnded<STATE, ENTITY>(
	val to: STATE,
	val msg: Msg,
	val entity: ENTITY,
) : AppEvent
		where STATE : S2State, ENTITY : WithS2State<STATE>

/**
 * Notification when transition started.
 *
 * @param transition the transition
 */
class AutomateTransitionStarted(
	val from: S2State,
	val msg: Msg,
) : AppEvent

/**
 * Notification when transition ended.
 *
 * @param transition the transition
 */
class AutomateTransitionEnded<STATE, ENTITY>(
	val from: STATE,
	val to: STATE,
	val msg: Msg,
	val entity: ENTITY,
) : AppEvent
		where STATE : S2State, ENTITY : WithS2State<STATE>

/**
 * Notification when transition happened.
 *
 * @param transition the transition
 */
class AutomateTransitionError(
	val msg: Msg,
	val exception: Exception,
) : AppEvent

/**
 * Notification when automate starts
 *
 * @param automate the automate
 */
class AutomateSessionStarted<AUTOMATE>(
	val automate: AUTOMATE,
) : AppEvent

/**
 * Notification when automate stops
 *
 * @param automate the automate
 */
class AutomateSessionStopped<AUTOMATE>(
	val automate: AUTOMATE,
) : AppEvent

/**
 * Notification when automate enters error.
 *
 * @param automate the automate
 * @param exception the exception
 */
class AutomateSessionError(
	val automate: S2Automate,
	val exception: Exception?,
) : AppEvent
