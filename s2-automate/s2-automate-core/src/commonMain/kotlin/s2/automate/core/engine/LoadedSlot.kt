package s2.automate.core.engine

import f2.dsl.cqrs.envelope.Envelope
import s2.automate.core.persist.LoadOutcome
import s2.automate.core.persist.PersistOutcome

/**
 * Engine-internal pairing of a command envelope with the outcome of attempting
 * to load its target entity. The withOutcomes path produces a list of these
 * per chunk so [partitionTransitions] can route each item per-item:
 *  - [Ready] proceeds through `buildAppliedContext` / persist;
 *  - [Failed] surfaces directly in the outcome flow without ever calling the
 *    decide lambda.
 *
 * `Failed.failure.msgId` is the envelope id (== msgId per F2 wrapping), so the
 * outcome is correlated by the caller's msgId, not the domain entity id.
 */
sealed interface LoadedSlot<COMMAND, ENTITY> {
    val cmd: Envelope<COMMAND>

    data class Ready<COMMAND, ENTITY>(
        override val cmd: Envelope<COMMAND>,
        val entity: ENTITY,
    ) : LoadedSlot<COMMAND, ENTITY>

    data class Failed<COMMAND, ENTITY>(
        override val cmd: Envelope<COMMAND>,
        val failure: PersistOutcome.Failure<Nothing>,
    ) : LoadedSlot<COMMAND, ENTITY>
}

/**
 * Promotes a [LoadOutcome.Failure] to the corresponding [PersistOutcome.Failure]
 * of the same category, preserving the original [S2Error]. The engine uses this
 * to surface load-classified failures into the persist-shaped outcome flow
 * without losing the per-id classification.
 *
 * Direct match on the sealed [LoadOutcome.Failure] subtypes so the compiler
 * enforces exhaustiveness — if a new failure variant is added to [LoadOutcome]
 * the mapping here must be updated. The four subtypes are shape-aligned 1:1
 * with [PersistOutcome.Failure]'s four subtypes.
 *
 * The result is typed `PersistOutcome.Failure<Nothing>` so callers can up-cast
 * to any `PersistOutcome<EVENT>` (a Failure carries no event, so the EVENT type
 * parameter is unconstrained).
 */
fun LoadOutcome.Failure<*, *>.toPersistFailure(msgId: String): PersistOutcome.Failure<Nothing> =
    when (this) {
        is LoadOutcome.Rejected      -> PersistOutcome.Rejected(msgId, error)
        is LoadOutcome.Transient     -> PersistOutcome.Transient(msgId, error)
        is LoadOutcome.Indeterminate -> PersistOutcome.Indeterminate(msgId, error)
        is LoadOutcome.Conflict      -> PersistOutcome.Conflict(msgId, error)
    }
