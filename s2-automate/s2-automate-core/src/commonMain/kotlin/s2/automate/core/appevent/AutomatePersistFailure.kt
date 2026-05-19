package s2.automate.core.appevent

import s2.automate.core.persist.ErrorCategory

/**
 * Notification when a PersistOutcome.Failure is produced by the persister.
 *
 * Symmetric to the success-path events ([AutomateTransitionEnded] /
 * [AutomateInitTransitionEnded]): one event per failed outcome so
 * observers can implement dead-letter, alerting, or metrics.
 *
 * @param commandId the command that triggered the transition
 * @param errorCategory retry/remediation policy discriminator
 * @param errorCode machine-readable code from the persister
 * @param errorMessage human-readable description from the persister
 */
data class AutomatePersistFailure(
    val commandId: String,
    val errorCategory: ErrorCategory,
    val errorCode: String,
    val errorMessage: String,
) : AppEvent
