package s2.dsl.automate.model

import kotlin.js.JsExport

@JsExport
interface WithS2Iteration {
	fun s2Iteration(): Int

	/**
	 * Returns a copy of this entity stamped with [iteration]. Called by the
	 * storing persister at load time to carry the on-chain iteration onto the
	 * loaded entity, so the persist phase can reuse it instead of re-querying
	 * the chain.
	 *
	 * Implementations should return a copy via a covariant return type — declare
	 * the concrete entity type so callers keep type information and avoid casts:
	 * `override fun withS2Iteration(iteration: Int): MyEntity = copy(iteration = iteration)`.
	 */
	fun withS2Iteration(iteration: Int): WithS2Iteration
}

