package s2.automate.core.persist

/**
 * Generic error-kind discriminator that drives the kind of remediation.
 * Specific reason (peer name, gRPC code, validation code, etc.) lives in
 * `errorCode` + `errorMessage` on the PersistOutcome.Failure.
 *
 * See tasks/blockchain/ERROR-PROPAGATION.md §5.1 for the canonical envelope.
 */
enum class ErrorClass {
    OK,
    BUSINESS,
    AUTH,
    INPUT,
    NETWORK,
    STATE,
    INFRA,
    UNKNOWN,
}
