package s2.automate.core.appevent

import s2.dsl.automate.Msg
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2State

interface AppEvent

/**
 * Notification when state is entered.
 */
data class AutomateStateEntered(
    val state: S2State,
) : AppEvent

/**
 * Notification when state is exited.
 */
data class AutomateStateExited(
    val state: S2State,
) : AppEvent

/**
 * Notification when command was not accepted.
 */
// Not a data class: printing an event must not print the command or the entity it carries.
@Suppress("UseDataClass")
class AutomateTransitionNotAccepted(
    val from: S2State?,
    val msg: Msg,
) : AppEvent

/**
 * Notification when init transition started.
 */
// Not a data class: printing an event must not print the command or the entity it carries.
@Suppress("UseDataClass")
class AutomateInitTransitionStarted(
    val msg: Msg,
) : AppEvent

/**
 * Notification when init transition ended.
 */
// Not a data class: printing an event must not print the command or the entity it carries.
@Suppress("UseDataClass")
class AutomateInitTransitionEnded<STATE, ENTITY>(
    val to: STATE,
    val msg: Msg,
    val entity: ENTITY,
) : AppEvent
        where STATE : S2State, ENTITY : WithS2State<STATE>

/**
 * Notification when transition started.
 */
// Not a data class: printing an event must not print the command or the entity it carries.
@Suppress("UseDataClass")
class AutomateTransitionStarted(
    val from: S2State,
    val msg: Msg,
) : AppEvent

/**
 * Notification when transition ended.
 */
// Not a data class: printing an event must not print the command or the entity it carries.
@Suppress("UseDataClass")
class AutomateTransitionEnded<STATE, ENTITY>(
    val from: STATE,
    val to: STATE,
    val msg: Msg,
    val entity: ENTITY,
) : AppEvent
        where STATE : S2State, ENTITY : WithS2State<STATE>

/**
 * Notification when transition happened.
 */
// Not a data class: printing an event must not print the command or the entity it carries.
@Suppress("UseDataClass")
class AutomateTransitionError(
    val msg: Msg,
    val exception: Exception,
) : AppEvent

/**
 * Notification when automate starts.
 */
data class AutomateSessionStarted<AUTOMATE>(
    val automate: AUTOMATE,
) : AppEvent

/**
 * Notification when automate stops.
 */
data class AutomateSessionStopped<AUTOMATE>(
    val automate: AUTOMATE,
) : AppEvent

/**
 * Notification when automate enters error.
 */
data class AutomateSessionError(
    val automate: S2Automate,
    val exception: Exception,
) : AppEvent
