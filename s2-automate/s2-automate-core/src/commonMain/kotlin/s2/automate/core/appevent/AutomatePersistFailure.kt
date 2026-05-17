package s2.automate.core.appevent

import s2.automate.core.persist.ErrorClass
import s2.automate.core.persist.ErrorOrigin

/**
 * Notification when a PersistOutcome.Failure is produced by the persister.
 *
 * Symmetric to the success-path events ([AutomateTransitionEnded] /
 * [AutomateInitTransitionEnded]): one event per failed outcome so
 * observers can implement dead-letter, alerting, or metrics.
 *
 * @param commandId the command that triggered the transition
 * @param errorCategory discriminator: "Rejected" | "Transient" | "Indeterminate" | "Conflict"
 * @param errorCode machine-readable code from the persister
 * @param errorMessage human-readable description from the persister
 * @param errorClass broad error kind (business, auth, network, …)
 * @param errorOrigin subsystem that produced the failure
 */
data class AutomatePersistFailure(
    val commandId: String,
    val errorCategory: String,
    val errorCode: String,
    val errorMessage: String,
    val errorClass: ErrorClass,
    val errorOrigin: ErrorOrigin,
) : AppEvent
