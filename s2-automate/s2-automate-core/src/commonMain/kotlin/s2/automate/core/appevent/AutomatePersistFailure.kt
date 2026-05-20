package s2.automate.core.appevent

import s2.automate.core.persist.ErrorCategory
import s2.dsl.automate.S2Error

/**
 * Notification when a PersistOutcome.Failure is produced by the persister.
 *
 * Symmetric to the success-path events ([AutomateTransitionEnded] /
 * [AutomateInitTransitionEnded]): one event per failed outcome so
 * observers can implement dead-letter, alerting, or metrics.
 *
 * @param commandId the command that triggered the transition
 * @param category retry/remediation policy discriminator
 * @param error structured error carrying type, description, payload, and optional cause
 */
data class AutomatePersistFailure(
    val commandId: String,
    val category: ErrorCategory,
    val error: S2Error,
) : AppEvent
