package s2.dsl.automate

import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * Retry/remediation taxonomy shared across every Outcome-style result in the
 * s2 / c2 stack (PersistOutcome, LoadOutcome, TxOutcome, …). Consumers key
 * their retry policy off this category, not the specific [S2Error.type].
 *
 *  - [Rejected]      permanent failure; do not retry
 *  - [Transient]     temporary failure (network / timeout / throttle); retry with backoff
 *  - [Indeterminate] unclear if the operation landed; state-check then retry
 *  - [Conflict]      concurrent write conflict (MVCC); refresh state then retry
 *
 * Success outcomes do not carry a category — they are represented by the
 * Outcome's dedicated Success / Loaded / Committed subtype.
 *
 * Lives in the DSL module (not s2-automate-core) so the lower-level Fabric
 * client (chaincode-api-fabric) can use the same enum without taking a
 * dependency on the engine.
 */
@JsExport
@JsName("ErrorCategory")
enum class ErrorCategory {
    Rejected, Transient, Indeterminate, Conflict,
}
