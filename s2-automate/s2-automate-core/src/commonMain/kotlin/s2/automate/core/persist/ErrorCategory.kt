package s2.automate.core.persist

/**
 * Drives the retry/remediation policy at the consumer layer.
 *  - [Committed]: tx landed; mark SUCCESS
 *  - [Rejected]: permanent failure; do not retry
 *  - [Transient]: temporary failure (network/timeout/throttle); retry with backoff
 *  - [Indeterminate]: unclear if tx landed; state-check then retry
 *  - [Conflict]: concurrent write conflict (MVCC); refresh state then retry
 *
 * The specific reason for a failure (peer/code/etc.) lives in [PersistOutcome.Failure.errorCode].
 */
enum class ErrorCategory {
    Committed, Rejected, Transient, Indeterminate, Conflict,
}

/**
 * Bridges the sealed [PersistOutcome.Failure] subtype hierarchy to the [ErrorCategory] enum.
 * Useful for emitting wire/event types that carry a flat category discriminator.
 */
val PersistOutcome.Failure<*>.category: ErrorCategory get() = when (this) {
    is PersistOutcome.Rejected -> ErrorCategory.Rejected
    is PersistOutcome.Transient -> ErrorCategory.Transient
    is PersistOutcome.Indeterminate -> ErrorCategory.Indeterminate
    is PersistOutcome.Conflict -> ErrorCategory.Conflict
}
