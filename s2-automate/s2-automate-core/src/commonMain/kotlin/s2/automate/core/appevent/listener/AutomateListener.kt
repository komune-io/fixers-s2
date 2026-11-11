package s2.automate.core.appevent.listener

import s2.automate.core.appevent.AutomateInitTransitionEnded
import s2.automate.core.appevent.AutomateInitTransitionStarted
import s2.automate.core.appevent.AutomateSessionError
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

// One callback per automate event, twelve events: splitting the listener would split its implementations.
@Suppress("TooManyFunctions", "ComplexInterface")
interface AutomateListener<STATE, ID : Any, ENTITY, AUTOMATE>
        where STATE : S2State, ENTITY : WithS2State<STATE>, ENTITY : WithS2Id<ID> {

    /**
     * Notified when state is entered.
     */
    fun automateStateEntered(event: AutomateStateEntered)

    /**
     * Notified when state is exited.
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
     */
    fun automateInitTransitionStarted(event: AutomateInitTransitionStarted)

    /**
     * Notified when transition ended.
     */
    fun automateInitTransitionEnded(event: AutomateInitTransitionEnded<STATE, ENTITY>)

    /**
     * Notified when transition started.
     */
    fun automateTransitionStarted(event: AutomateTransitionStarted)

    /**
     * Notified when transition ended.
     */
    fun automateTransitionEnded(event: AutomateTransitionEnded<STATE, ENTITY>)

    /**
     * Notified when transition happened.
     */
    fun automateTransitionError(event: AutomateTransitionError)

    /**
     * Notified when statemachine starts.
     */
    fun automateSessionStarted(event: AutomateSessionStarted<AUTOMATE>)

    /**
     * Notified when statemachine stops.
     */
    fun automateSessionStopped(event: AutomateSessionStopped<AUTOMATE>)

    /**
     * Notified when statemachine enters error it can't recover from.
     */
    fun automateSessionError(event: AutomateSessionError)

    /**
     * Notified when a persist operation fails.
     *
     * @param event the persist failure event
     */
    fun automatePersistFailure(event: AutomatePersistFailure)
}
