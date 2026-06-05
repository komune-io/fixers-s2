package s2.automate.core.persist

import s2.dsl.automate.S2Error

/**
 * Per-id outcome of attempting to load an entity from storage. Mirrors the
 * shape of [PersistOutcome] (Success-or-Failure with a shared [ErrorCategory])
 * but is specialised for the read direction:
 *  - the success variant carries the entity directly (no msgId / metadata —
 *    load works in domain-id space, not envelope/correlation-id space);
 *  - the failure subtypes are a subset of [PersistOutcome]'s — no [Conflict]
 *    (concurrent reads don't conflict) and no [Indeterminate] (load is
 *    read-only: either the read returned data or it errored).
 *
 * The engine promotes [LoadOutcome.Failure] to the corresponding
 * [PersistOutcome.Failure] of the same [category] when surfacing through the
 * outcome flow, so downstream consumers see a single uniform taxonomy.
 */
sealed interface LoadOutcome<ID, ENTITY> {
    val id: ID

    sealed interface Failure<ID, ENTITY> : LoadOutcome<ID, ENTITY> {
        val error: S2Error
        val category: ErrorCategory
    }

    /** Entity was found and successfully materialised. */
    data class Loaded<ID, ENTITY>(
        override val id: ID,
        val entity: ENTITY,
    ) : LoadOutcome<ID, ENTITY>

    /**
     * Permanent failure — no such entity, or the stored payload is unusable
     * (e.g. deserialisation failure on a corrupt write). Engine maps to
     * [PersistOutcome.Rejected]; consumers should not retry.
     */
    data class Rejected<ID, ENTITY>(
        override val id: ID,
        override val error: S2Error,
    ) : Failure<ID, ENTITY> {
        override val category: ErrorCategory get() = ErrorCategory.Rejected
    }

    /**
     * Transient failure — network / timeout / throttle. Engine maps to
     * [PersistOutcome.Transient]; consumers should retry with backoff.
     */
    data class Transient<ID, ENTITY>(
        override val id: ID,
        override val error: S2Error,
    ) : Failure<ID, ENTITY> {
        override val category: ErrorCategory get() = ErrorCategory.Transient
    }
}
