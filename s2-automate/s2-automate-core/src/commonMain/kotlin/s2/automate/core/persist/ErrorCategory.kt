package s2.automate.core.persist

/**
 * Drives the retry/remediation policy at the consumer layer for [PersistOutcome.Failure] outcomes.
 *  - [Rejected]: permanent failure; do not retry
 *  - [Transient]: temporary failure (network/timeout/throttle); retry with backoff
 *  - [Indeterminate]: unclear if tx landed; state-check then retry
 *  - [Conflict]: concurrent write conflict (MVCC); refresh state then retry
 *
 * The specific reason for a failure lives in [PersistOutcome.Failure.error].
 * Success outcomes do not carry a category — they are represented by [PersistOutcome.Success].
 */
enum class ErrorCategory {
    Rejected, Transient, Indeterminate, Conflict,
}
