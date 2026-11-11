package s2.automate.core.persist

import s2.automate.core.appevent.AppEvent
import s2.dsl.automate.ErrorCategory
import s2.dsl.automate.S2Error

/**
 * Notification when a PersistOutcome.Failure is produced by the persister.
 *
 * Symmetric to the success-path events (AutomateTransitionEnded /
 * AutomateInitTransitionEnded): one event per failed outcome so
 * observers can implement dead-letter, alerting, or metrics.
 *
 * @property msgId the message id that triggered the transition
 * @property category retry/remediation policy discriminator
 * @property error structured error carrying type, description, payload, and optional cause
 */
data class AutomatePersistFailure(
    val msgId: String,
    val category: ErrorCategory,
    val error: S2Error,
) : AppEvent
