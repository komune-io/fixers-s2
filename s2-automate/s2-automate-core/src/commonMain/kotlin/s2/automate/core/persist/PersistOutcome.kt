package s2.automate.core.persist

/**
 * Per-item result of a persist operation. Replaces the previous fail-fast
 * `Flow<EVENT>` semantics: each input context now yields exactly one
 * `PersistOutcome` so partial-batch failures are visible to the caller.
 *
 * Categories follow `tasks/blockchain/error-management.html` §04.
 */
sealed interface PersistOutcome<EVENT> {
    val commandId: String

    /** Transaction reached the ledger and validated successfully. */
    data class Committed<EVENT>(
        override val commandId: String,
        val event: EVENT,
        val transactionId: String,
        val blockNumber: Long,
    ) : PersistOutcome<EVENT>

    /** Will never succeed without intervention (bad iteration, forbidden transition, signature invalid, missing session). */
    data class Rejected<EVENT>(
        override val commandId: String,
        val errorCode: String,
        val errorMessage: String,
    ) : PersistOutcome<EVENT>

    /** Infra-level failure (conn refused, timeout, gateway 5xx). Safe to retry. */
    data class Transient<EVENT>(
        override val commandId: String,
        val errorCode: String,
        val errorMessage: String,
    ) : PersistOutcome<EVENT>

    /** Submitted but outcome unknown. Caller must query state before retrying. */
    data class Indeterminate<EVENT>(
        override val commandId: String,
        val errorCode: String,
        val errorMessage: String,
    ) : PersistOutcome<EVENT>

    /** Tx in a block but INVALID (MVCC_READ_CONFLICT, ENDORSEMENT_POLICY_FAILURE, etc.). Re-read state and re-submit. */
    data class Conflict<EVENT>(
        override val commandId: String,
        val errorCode: String,
        val errorMessage: String,
    ) : PersistOutcome<EVENT>

    fun eventOrNull(): EVENT? = (this as? Committed<EVENT>)?.event
}
