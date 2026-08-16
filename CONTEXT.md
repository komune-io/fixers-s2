# fixers-s2 — Context

S2 models **the lifecycle of a domain entity as a finite state machine**. It provides the DSL to define the state machine, the engine that executes transitions, and two interchangeable persistence strategies (event-sourced or state-stored) for landing the result.

S2 sits directly on top of F2: the messages that drive transitions are F2's CQRS Commands and Events, refined with state-machine metadata.

## Glossary

### Automate

The S2 term — borrowed from French — for a **finite state machine definition**: `S2Automate` (in `s2-automate-dsl`, package `s2.dsl.automate`) is a name, an optional version, and the set of valid `S2Transition`s between `S2State`s for one entity type. Treat "Automate" and "automaton" as synonyms in prose; **"Automate" is the canonical written form** because that is what the code, artifact names (`s2-automate-*`), and types (`S2Automate`) use. Plural: Automates. Automates can nest: `S2SubMachine` wraps an Automate with start/end triggers.

An Automate is a *definition*, not an instance. An instance of an entity following an Automate is just called the **aggregate** (using DDD vocabulary); aggregates expose their identity and current state through the `WithS2Id<ID>` / `WithS2State<STATE>` markers.

*Avoid*: "state machine" / "FSM" as the term of art (fine as an explanation, as in the README, but the types and artifacts say Automate); "automata".

### S2State

One position in an Automate: an interface with a single `val position: Int`, a stable index the engine uses to compare states (two states are "the same" when their positions match). Each Automate enumerates its States.

### S2Transition

A directed edge in the Automate. Its real shape is `S2Transition(from, to, role, action, result)`: `from` — source state (nullable), `to` — target state, `role` — who may trigger it, `action` — the Command type that triggers it, `result` — the Event type it produces (optional). A transition with `from = null` is an **init transition** (creates the aggregate; the DSL also has a dedicated `S2InitTransition` type with `to`/`role`/`action`). Declared in the Automate DSL builder; the engine accepts a Command only if a transition whose `action` matches leaves the aggregate's current state.

### S2Command

A request to drive an aggregate through a transition: `S2Command<ID> : Cmd, WithId<ID>`, where `Cmd` is S2's typealias for f2-dsl-cqrs's `Command` and `WithId<ID>` is S2's own identifier marker. The companion **S2InitCommand** creates the aggregate: it extends `Cmd` only — no id, because the aggregate does not exist yet.

### S2Event

The result of executing an S2Command: `S2Event<STATE, ID> : Evt, WithId<ID>` (`Evt` = f2's `Event`) adds the aggregate `id` and `type: STATE`, the state the aggregate lands in. Two standalone classes (implementing `Evt` directly, not `S2Event`) report transition outcomes:

- **S2EventSuccess** — `id`, the triggering command (`type`), and `from`/`to` states.
- **S2EventError** — the same, plus an `error: S2Error` (type, description, date, payload).

### S2Role

A permission marker interface attached to every Transition, declaring who is allowed to trigger it. It is declarative metadata serialized with the Automate definition, left to consumers to enforce; the core engine's guards check state/transition validity, not roles.

### Guard

A condition the engine evaluates before and after each transition (`Guard` / `GuardVerifier` in s2-automate-core). The built-in `TransitionStateGuard` rejects a Command when no transition with a matching `action` leaves the aggregate's current state.

### S2AutomateEngine

The engine entry point (s2-automate-core): `create(initCommands, decide)` and `doTransition(commands, exec)` load the aggregate, run the Guards, execute the consumer's logic, and hand the result to an `AutomatePersister`. Consumers reach it through the Spring adapters `S2AutomateExecutorSpring` (storing) and `S2AutomateDeciderSpring` (sourcing).

### Decide

`Decide<COMMAND, EVENT>`: an `F2Function<COMMAND, EVENT>` holding the business logic that turns a Command into an Event. Defined in s2-event-sourcing-dsl but returned by both modes' `decide { }` / `transition(command) { }` builders, which the engine calls with the current aggregate in scope.

### Evolve

The conceptual update step: applying an Event to a model to produce the next model (a `fun interface Evolve<EVENT, ENTITY> : F2Function<EVENT, ENTITY>` exists in s2-event-sourcing-dsl). In code the replay fold is `View.evolve`; state recovery by replay is not used in storing mode.

### View

Sourcing: `View<EVENT, ENTITY>` with `suspend fun evolve(event, model): ENTITY?` — the fold that rebuilds an aggregate (or a read projection) from its event log. `ViewLoader` applies it over an `EventRepository`.

### Loader

Sourcing only: `Loader<EVENT, ENTITY, ID>` (s2-event-sourcing-dsl) materialises an aggregate in its current state. `ViewLoader` replays the event log through a View; `SnapLoader` reads a Snapshot first and falls back to replay. In storing mode, loading is done by the `AutomatePersister` instead.

### Snapshot (Snap)

Sourcing optimisation: a cached copy of the current aggregate kept in a `SnapRepository` so `SnapLoader` can skip full event replay. A Snapshot is a cache over the event log, not the source of truth — do not confuse it with Storing mode.

### Persistence strategy: Sourcing vs Storing

Same Automate, Command, State, and Event definitions in both cases; the difference is **how the current state is recovered**.

- **Sourcing** (`s2-event-sourcing-dsl`, `s2-spring-boot-starter-sourcing`, event store via `s2-spring-boot-starter-sourcing-data` with `-mongodb` / `-r2dbc` variants) — store every Event, rebuild the aggregate by replay (View/Loader). Choose when an immutable audit log is required.
- **Storing** (`s2-spring-boot-starter-storing`, Spring Data persistence via `s2-spring-boot-starter-storing-data`) — persist the current aggregate directly; the consumer returns the updated entity alongside the event (`S2AutomateStoringEvolver.doTransition`). Choose when the audit log is not load-bearing and you want simpler reads. No event replay in this mode.

The choice is per-Automate and made at Spring configure time by picking which starter (and `S2ConfigurerAdapter`) to depend on.

### S2Documenter

`s2-automate-documenter` serializes an `S2Automate` definition to JSON (`build/s2-documenter/<name>.json`) so automate documentation and diagrams can be generated from the real definition rather than by hand.

## Cross-references

- Inherits Command / Query / Event from [../fixers-f2/CONTEXT.md](../fixers-f2/CONTEXT.md).
- Specialised further by [../fixers-c2/CONTEXT.md](../fixers-c2/CONTEXT.md) (SSM = Signing State Machine on Hyperledger Fabric — an Automate on a blockchain).
- Used to model the file lifecycle in [../../connect/connect-fs/CONTEXT.md](../../connect/connect-fs/CONTEXT.md).
- Layer position: [../../docs/adr/0001-submodule-dependency-layers.md](../../docs/adr/0001-submodule-dependency-layers.md).
