package s2.automate.core.persist

import s2.dsl.automate.ErrorCategory
import s2.dsl.automate.S2Error

/**
 * Per-id outcome of attempting to load an entity from storage. Shape-aligned
 * with [PersistOutcome] (Success-or-Failure with the same [ErrorCategory]
 * taxonomy on every failure subtype) but specialised for the read direction:
 * the success variant carries the entity directly and is keyed by the domain
 * `ID` type rather than the envelope's stringly-typed `msgId`.
 *
 * All four [ErrorCategory] variants have a corresponding [Failure] subtype so
 * an implementer can emit any of them — the engine promotes each
 * [LoadOutcome.Failure] to the [PersistOutcome.Failure] of the same category
 * when surfacing through the outcome flow, so downstream consumers see one
 * uniform taxonomy. In practice the SSM persister currently emits only
 * Rejected (not-found / unusable payload) and Transient (network / timeout);
 * the Indeterminate and Conflict variants exist so a future overrider can
 * distinguish "the read might be stale" or "concurrent schema migration"
 * without breaking the API.
 *
 * `ID` and `ENTITY` are declared as `out` since they only appear in
 * read-only positions — this lets callers safely upcast e.g. a
 * `LoadOutcome<UserId, User>` to a `LoadOutcome<EntityId, Any>` without casts.
 */
sealed interface LoadOutcome<out ID, out ENTITY> {
    val id: ID

    sealed interface Failure<out ID, out ENTITY> : LoadOutcome<ID, ENTITY> {
        val error: S2Error
        val category: ErrorCategory
    }

    /** Entity was found and successfully materialised. */
    data class Loaded<out ID, out ENTITY>(
        override val id: ID,
        val entity: ENTITY,
    ) : LoadOutcome<ID, ENTITY>

    /** Permanent failure — no such entity, or the stored payload is unusable. */
    data class Rejected<out ID, out ENTITY>(
        override val id: ID,
        override val error: S2Error,
    ) : Failure<ID, ENTITY> {
        override val category: ErrorCategory = ErrorCategory.Rejected
    }

    /** Transient failure — network / timeout / throttle. Consumer should retry. */
    data class Transient<out ID, out ENTITY>(
        override val id: ID,
        override val error: S2Error,
    ) : Failure<ID, ENTITY> {
        override val category: ErrorCategory = ErrorCategory.Transient
    }

    /**
     * Indeterminate failure — the read returned ambiguous state (partial
     * result, stale replica, etc.). Consumer should state-check and retry.
     */
    data class Indeterminate<out ID, out ENTITY>(
        override val id: ID,
        override val error: S2Error,
    ) : Failure<ID, ENTITY> {
        override val category: ErrorCategory = ErrorCategory.Indeterminate
    }

    /**
     * Conflict failure — a concurrent writer (schema migration, lock contention)
     * prevented a clean read. Consumer should refresh state and retry.
     */
    data class Conflict<out ID, out ENTITY>(
        override val id: ID,
        override val error: S2Error,
    ) : Failure<ID, ENTITY> {
        override val category: ErrorCategory = ErrorCategory.Conflict
    }
}
