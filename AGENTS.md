## Project Overview

S2 = Kotlin multiplatform framework. Models domain object lifecycles via finite state machines (FSM) + CQRS. Supports event-sourced and state-stored approaches. Persists to databases (Spring Data MongoDB, R2DBC) or Hyperledger Fabric blockchain (Blockchain-SSM).

## Build Commands

```bash
# Build and publish to local Maven
VERSION=$(cat VERSION) ./gradlew clean build publishToMavenLocal -x test

# Run linting
./gradlew detekt

# Run tests (requires dev environment)
./gradlew test

# Run a single test class
./gradlew :module:path:test --tests "FullyQualifiedClassName"

# Run a single test method
./gradlew :module:path:test --tests "FullyQualifiedClassName.methodName"
```

## Dev Environment (Docker)

Tests need infrastructure (CouchDB for blockchain, etc.). Use Make targets:

```bash
# Start all dev services
make dev up

# Initialize blockchain (required before tests)
make dev bclan-init logs

# Stop services
make dev down

# View logs
make dev logs
```

## Module Architecture

### Core Libraries (Kotlin Multiplatform)
- `s2-automate/s2-automate-dsl`: DSL for defining state machines - states, roles, commands, events, transitions
- `s2-automate/s2-automate-core`: State machine execution engine, guards, persisters
- `s2-event-sourcing/s2-event-sourcing-dsl`: Event sourcing abstractions - `View`, `Loader`, `EventRepository`, `SnapRepository`

### Spring Boot Integration
- `s2-spring/s2-spring-core`: Base Spring adapter, event publishing
- `s2-spring/storing/s2-spring-boot-starter-storing`: State-stored approach (persist entity state directly)
- `s2-spring/storing/s2-spring-boot-starter-storing-data`: Spring Data integration for state storing
- `s2-spring/sourcing/s2-spring-boot-starter-sourcing`: Event-sourced approach (persist events, rebuild state)
- `s2-spring/sourcing/s2-spring-boot-starter-sourcing-data-mongodb`: MongoDB event store
- `s2-spring/sourcing/s2-spring-boot-starter-sourcing-data-r2dbc`: R2DBC event store

### Testing
- `s2-test/s2-test-bdd`: Cucumber BDD test utilities

### Samples
- `sample/orderbook-sourcing`: Event sourcing examples with MongoDB, R2DBC
- `sample/orderbook-storing`: State storing example
- `sample/multiautomate`: Multiple automata example

## Key Concepts

### Defining a State Machine

```kotlin
// 1. Define states (implements S2State with position)
enum class MyState(override var position: Int): S2State {
    Created(0), Active(1), Closed(2)
}

// 2. Define commands (S2InitCommand for creation, S2Command<ID> for transitions)
data class CreateCommand(val name: String) : S2InitCommand
data class ActivateCommand(override val id: String) : S2Command<String>

// 3. Define events (include state for event sourcing)
data class CreatedEvent(val id: String, val state: MyState) : Evt, WithS2Id<String>, WithS2State<MyState>

// 4. Build the automate
val myAutomate = s2 {
    name = "MyAutomate"
    transaction<CreateCommand> {
        to = MyState.Created
        role = MyRole
        evt = CreatedEvent::class
    }
    transaction<ActivateCommand> {
        from = MyState.Created
        to = MyState.Active
        role = MyRole
        evt = ActivatedEvent::class
    }
    selfTransaction<UpdateCommand> {  // stays in same state
        states += MyState.Created
        role = MyRole
        evt = UpdatedEvent::class
    }
}
```

### Event Sourcing Implementation

Extend `S2AutomateDeciderSpringAdapter`, implement:
- `automate()`: Return S2Automate definition
- `eventStore()`: Provide EventRepository implementation
- `entityType()`: Return event KClass
- Create `View<EVENT, ENTITY>` to rebuild entity from events

### State Storing Implementation

Extend `S2ConfigurerAdapter`, implement:
- `automate()`: Return S2Automate definition
- `aggregateRepository()`: Provide AutomatePersister
- `executor()`: Return S2AutomateExecutorSpring

## Configuration

Version from `VERSION` file. Build uses `buildSrc` with Komune fixers gradle plugin for dependency management.

Local maven dependencies:
```bash
MAVEN_LOCAL_USE=true ./gradlew build
```