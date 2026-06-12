# fixers-s2 — Context

S2 models **the lifecycle of a domain entity as a finite state machine**. It provides the DSL to define the state machine, the engine that executes transitions, and two interchangeable persistence strategies (event-sourced or state-stored) for landing the result.

S2 sits directly on top of F2: the messages that drive transitions are F2's CQRS Commands and Events, refined with state-machine metadata.

## Glossary

### Automate

The S2 term — borrowed from French — for a **finite state machine definition**: a name + an ordered set of valid `Transition`s between `State`s for one entity type. Treat "Automate" and "automaton" as synonyms in prose; **"Automate" is the canonical written form** because that is what the code, artifact names (`s2-automate-*`), and types (`S2Automate`) use. Plural: Automates.

An Automate is a *definition*, not an instance. An instance of an entity following an Automate is just called the **aggregate** (using DDD vocabulary).

### S2State

A marker type for one position in an Automate. Implements `S2State` and carries a stable position index so the engine can compare ordering. Each Automate enumerates its States.

### S2Transition

A directed edge in the Automate: `(fromState, command, role) → (toState, event)`. Declared in the Automate DSL; executed by the engine when a matching Command arrives for an aggregate currently in `fromState`.

### S2Command

A request to drive an aggregate through a transition. Refines f2-dsl-cqrs's `Cmd` with a `WithId<ID>` aggregate identifier (`S2Command<ID> : Cmd, WithId<ID>`). The companion **S2InitCommand** is the special variant that creates the aggregate (no prior state).

### S2Event

The result of executing an S2Command. Refines f2-dsl-cqrs's `Evt` with the aggregate id and the resulting state type (`S2Event<STATE, ID> : Evt, WithId<ID>`). Two concrete wrappers:

- **S2EventSuccess** — carries `from` and `to` states.
- **S2EventError** — carries `from`, `to`, and an `S2Error`.

### S2Role

A permission marker attached to a Transition. Restricts who is allowed to trigger that transition; checked by the engine at command dispatch.

### Decide

The function `(Command, currentState) → Event` that holds the business logic for one Transition. Authored by the consumer; called by the engine.

### Evolve

Event-sourcing only: the function `(Event, entityOrNull) → entity` that rebuilds an aggregate by folding the event log. Not used in state-stored mode.

### Loader

The component that materialises an aggregate in its current state for the engine to inspect before dispatch. In sourcing mode it replays events through Evolve; in storing mode it loads the snapshot row from the database.

### Persistence strategy: Sourcing vs Storing

Same Automate, Command, State, and Event definitions in both cases; the difference is **how the current state is recovered**.

- **Sourcing** (`s2-spring-boot-starter-sourcing`, `s2-event-sourcing-dsl`) — store every Event, replay through Evolve to rebuild the aggregate. Choose when an immutable audit log is required.
- **Storing** (`s2-spring-boot-starter-storing`) — persist the current aggregate snapshot (MongoDB / R2DBC variants). Choose when the audit log is not load-bearing and you want simpler reads. No Evolve function in this mode.

The choice is per-Automate and made at Spring autoconfigure time by picking which starter to depend on.

## Cross-references

- Inherits Command / Query / Event from [../fixers-f2/CONTEXT.md](../fixers-f2/CONTEXT.md).
- Specialised further by [../fixers-c2/CONTEXT.md](../fixers-c2/CONTEXT.md) (SSM = Signing State Machine on Hyperledger Fabric — an Automate on a blockchain).
- Used to model the file lifecycle in [../../connect/connect-fs/CONTEXT.md](../../connect/connect-fs/CONTEXT.md).
- Layer position: [../../docs/adr/0001-submodule-dependency-layers.md](../../docs/adr/0001-submodule-dependency-layers.md).
