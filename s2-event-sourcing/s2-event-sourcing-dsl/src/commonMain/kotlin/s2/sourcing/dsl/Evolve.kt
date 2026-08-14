package s2.sourcing.dsl

import f2.dsl.fnc.F2Function
import s2.dsl.automate.Evt

/**
 * Unused event-fold abstraction.
 *
 * Nothing in the framework ever builds or consumes an [Evolve]: the event fold actually
 * used by the sourcing engine is [s2.sourcing.dsl.view.View], whose `evolve(event, model)`
 * receives the current state alongside the event. [Evolve] cannot express that fold at all,
 * since an `F2Function<EVENT, ENTITY>` has no access to the previous state.
 *
 * Kept for one deprecation cycle because it is published API; it will be removed in a
 * future major release. Migrate to [s2.sourcing.dsl.view.View].
 */
@Deprecated(
	message = "Dead abstraction, never used by the sourcing engine. The real event fold is " +
		"s2.sourcing.dsl.view.View, which also receives the current state. Will be removed " +
		"in a future major release.",
	replaceWith = ReplaceWith("View<EVENT, ENTITY>", "s2.sourcing.dsl.view.View"),
	level = DeprecationLevel.WARNING,
)
fun interface Evolve<EVENT, ENTITY> : F2Function<EVENT, ENTITY> where
EVENT : Evt
