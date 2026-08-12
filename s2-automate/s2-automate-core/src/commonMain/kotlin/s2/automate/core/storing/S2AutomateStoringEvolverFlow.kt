package s2.automate.core.storing

import f2.dsl.cqrs.enveloped.EnvelopedFlow
import kotlinx.coroutines.flow.Flow
import s2.automate.core.persist.PersistOutcome
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2State
import s2.sourcing.dsl.Decide

typealias S2EvolveInitFnc<COMMAND, ENTITY, EVENT_OUT> = suspend (cmd: COMMAND) -> Pair<ENTITY, EVENT_OUT>
typealias S2EvolveFnc<COMMAND, ENTITY, EVENT_OUT> = suspend (COMMAND, ENTITY) -> Pair<ENTITY, EVENT_OUT>

/**
 * Flow-oriented storing API.
 *
 * ### Naming
 *
 * Historically every method here was called `evolve`, which collides with the event-sourcing
 * `evolve` (the `event -> state` fold of [s2.sourcing.dsl.view.View]) while doing something
 * entirely different: these methods take *commands*, run them through the automate and emit
 * *events*. That is a decide step, not a fold.
 *
 * Each `evolve*` method therefore has a clearer alias named after what it actually does —
 * [create]/[transition] and their `Envelope`/`WithOutcomes` variants — mirroring the vocabulary
 * already used by `S2AutomateEngine`. The aliases are `open` defaults delegating to the
 * `evolve*` methods, so existing implementations keep working untouched; the `evolve*` methods
 * are deprecated and will be removed in a future major release.
 */
@Suppress("DEPRECATION", "TooManyFunctions")
interface S2AutomateStoringEvolverFlow<STATE : S2State, ID, ENTITY : WithS2State<STATE>, EVENT: Evt> {

	@Deprecated(
		message = "Confusing name: this decides events from init commands, it is not a sourcing fold.",
		replaceWith = ReplaceWith("create(commands, build)"),
	)
	suspend fun <COMMAND: S2InitCommand, EVENT_OUT: EVENT> evolve(
		commands: Flow<COMMAND>,
		build: S2EvolveInitFnc<COMMAND, ENTITY, EVENT_OUT>
	): Flow<EVENT_OUT>

	@Deprecated(
		message = "Confusing name: this decides events from init commands, it is not a sourcing fold.",
		replaceWith = ReplaceWith("createEnvelope(commands, build)"),
	)
	suspend fun <COMMAND: S2InitCommand, EVENT_OUT: EVENT> evolveEnvelope(
		commands: EnvelopedFlow<COMMAND>,
		build: S2EvolveInitFnc<COMMAND, ENTITY, EVENT_OUT>
	): EnvelopedFlow<EVENT_OUT>

	@Deprecated(
		message = "Confusing name: this builds a Decide from an init command handler, it is not a sourcing fold.",
		replaceWith = ReplaceWith("decideCreate(build)"),
	)
	fun <COMMAND: S2InitCommand, EVENT_OUT: EVENT> evolve(
		build: S2EvolveInitFnc<COMMAND, ENTITY, EVENT_OUT>
	): Decide<COMMAND, EVENT_OUT>

	@Deprecated(
		message = "Confusing name: this decides events from transition commands, it is not a sourcing fold.",
		replaceWith = ReplaceWith("transition(commands, exec)"),
	)
	suspend fun <COMMAND: S2Command<ID>, EVENT_OUT: EVENT> evolve(
		commands: Flow<COMMAND>,
		exec: S2EvolveFnc<COMMAND, ENTITY, EVENT_OUT>
	): Flow<EVENT_OUT>

	@Deprecated(
		message = "Confusing name: this decides events from transition commands, it is not a sourcing fold.",
		replaceWith = ReplaceWith("transitionEnvelope(commands, exec)"),
	)
	suspend fun <COMMAND: S2Command<ID>, EVENT_OUT: EVENT> evolveEnvelope(
		commands: EnvelopedFlow<COMMAND>,
		exec: S2EvolveFnc<COMMAND, ENTITY, EVENT_OUT>
	): EnvelopedFlow<EVENT_OUT>

	@Deprecated(
		message = "Confusing name: this builds a Decide from a transition handler, it is not a sourcing fold.",
		replaceWith = ReplaceWith("decideTransition(fnc)"),
	)
	fun <COMMAND : S2Command<ID>, EVENT_OUT : EVENT> evolve(
		fnc: S2EvolveFnc<COMMAND, ENTITY, EVENT_OUT>
	): Decide<COMMAND, EVENT_OUT>

	@Deprecated(
		message = "Confusing name: this decides events from init commands, it is not a sourcing fold.",
		replaceWith = ReplaceWith("createWithOutcomes(commands, idOf, build)"),
	)
	suspend fun <COMMAND: S2InitCommand, EVENT_OUT: EVENT> evolveWithOutcomes(
		commands: Flow<COMMAND>,
		idOf: (COMMAND) -> String,
		build: S2EvolveInitFnc<COMMAND, ENTITY, EVENT_OUT>
	): Flow<PersistOutcome<EVENT_OUT>>

	@Deprecated(
		message = "Confusing name: this decides events from transition commands, it is not a sourcing fold.",
		replaceWith = ReplaceWith("transitionWithOutcomes(commands, idOf, exec)"),
	)
	suspend fun <COMMAND: S2Command<ID>, EVENT_OUT: EVENT> evolveWithOutcomes(
		commands: Flow<COMMAND>,
		idOf: (COMMAND) -> String,
		exec: S2EvolveFnc<COMMAND, ENTITY, EVENT_OUT>
	): Flow<PersistOutcome<EVENT_OUT>>

	/**
	 * Creates one entity per init command and emits the resulting events.
	 * Preferred name for [evolve].
	 */
	suspend fun <COMMAND: S2InitCommand, EVENT_OUT: EVENT> create(
		commands: Flow<COMMAND>,
		build: S2EvolveInitFnc<COMMAND, ENTITY, EVENT_OUT>
	): Flow<EVENT_OUT> = evolve(commands, build)

	/**
	 * Envelope-preserving variant of [create]. Preferred name for [evolveEnvelope].
	 */
	suspend fun <COMMAND: S2InitCommand, EVENT_OUT: EVENT> createEnvelope(
		commands: EnvelopedFlow<COMMAND>,
		build: S2EvolveInitFnc<COMMAND, ENTITY, EVENT_OUT>
	): EnvelopedFlow<EVENT_OUT> = evolveEnvelope(commands, build)

	/**
	 * Builds a [Decide] function creating entities from init commands.
	 * Preferred name for the [evolve] overload taking only a builder.
	 */
	fun <COMMAND: S2InitCommand, EVENT_OUT: EVENT> decideCreate(
		build: S2EvolveInitFnc<COMMAND, ENTITY, EVENT_OUT>
	): Decide<COMMAND, EVENT_OUT> = evolve(build)

	/**
	 * Applies one transition per command and emits the resulting events.
	 * Preferred name for [evolve].
	 */
	suspend fun <COMMAND: S2Command<ID>, EVENT_OUT: EVENT> transition(
		commands: Flow<COMMAND>,
		exec: S2EvolveFnc<COMMAND, ENTITY, EVENT_OUT>
	): Flow<EVENT_OUT> = evolve(commands, exec)

	/**
	 * Envelope-preserving variant of [transition]. Preferred name for [evolveEnvelope].
	 */
	suspend fun <COMMAND: S2Command<ID>, EVENT_OUT: EVENT> transitionEnvelope(
		commands: EnvelopedFlow<COMMAND>,
		exec: S2EvolveFnc<COMMAND, ENTITY, EVENT_OUT>
	): EnvelopedFlow<EVENT_OUT> = evolveEnvelope(commands, exec)

	/**
	 * Builds a [Decide] function applying transitions.
	 * Preferred name for the [evolve] overload taking only a transition handler.
	 */
	fun <COMMAND : S2Command<ID>, EVENT_OUT : EVENT> decideTransition(
		fnc: S2EvolveFnc<COMMAND, ENTITY, EVENT_OUT>
	): Decide<COMMAND, EVENT_OUT> = evolve(fnc)

	/**
	 * [create] reporting a per-command [PersistOutcome] instead of failing the whole flow.
	 * Preferred name for [evolveWithOutcomes].
	 */
	suspend fun <COMMAND: S2InitCommand, EVENT_OUT: EVENT> createWithOutcomes(
		commands: Flow<COMMAND>,
		idOf: (COMMAND) -> String,
		build: S2EvolveInitFnc<COMMAND, ENTITY, EVENT_OUT>
	): Flow<PersistOutcome<EVENT_OUT>> = evolveWithOutcomes(commands, idOf, build)

	/**
	 * [transition] reporting a per-command [PersistOutcome] instead of failing the whole flow.
	 * Preferred name for [evolveWithOutcomes].
	 */
	suspend fun <COMMAND: S2Command<ID>, EVENT_OUT: EVENT> transitionWithOutcomes(
		commands: Flow<COMMAND>,
		idOf: (COMMAND) -> String,
		exec: S2EvolveFnc<COMMAND, ENTITY, EVENT_OUT>
	): Flow<PersistOutcome<EVENT_OUT>> = evolveWithOutcomes(commands, idOf, exec)

}
