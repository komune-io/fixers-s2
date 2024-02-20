# S2

S2 is a Kotlin multiplatform framework that represents domain object life cycles using finite state machines,
and implements all transactions as commands using the Command Query Responsibility Segregation (CQRS) pattern.
It can store state or events in a database using Spring Data or Hyperledger Fabric Blockchain using Blockchain-SSM.
S2 uses finite state machines to model and manage the state of aggregates and entities,
and CQRS to separate read and write operations. It is useful in Domain-Driven Design (DDD) for modeling
the state transitions of aggregates or entities. S2 offers two different approaches for managing
the state of a software system: event-sourced and state-stored.

The key features of the S2 framework include:

* Representing Domain Object Life Cycle using Finite State Machine:  
  The framework provides a robust mechanism for representing the life cycle of domain objects using finite state machines.
  Developers can use finite state machines to describe state transitions and ensure that objects remain
  in a valid state throughout their lifecycle.

* Building Models using State Storing or Event Sourcing using CQRS:   
  The framework provides support for building models using state storing or event sourcing using
  the Command Query Responsibility Segregation pattern. Developers can use this pattern
  to separate read and write operations, improve scalability, and optimize performance.

* Storing State or Event in a Database using Spring Data or in Hyperledger Fabric Blockchain using Blockchain-SSM:  
  The framework provides support for storing state or events in a database using Spring Data
  or in Hyperledger Fabric Blockchain using Blockchain-SSM.
  This allows developers to choose the best data storage mechanism for their specific use case.

Overall, the S2 framework provides developers with a powerful and flexible set of tools for modeling
and managing the state of domain objects. By leveraging the concepts of finite state machines, CQRS,
and domain-driven development, developers can create robust, reliable software systems that meet the needs of their users.


## Get Started

### Define State Machine

* import kotlin multiplatform dependency
```kotlin
implementation("io.komune.s2:s2-automate-dsl:${Versions.s2}")
```

* Define state
```kotlin
open class DidState(
	override val position: Int,
) : S2State {
	open class Created : DidState(0)
	open class Actived : DidState(1)
	open class Revoked : DidState(2)
}
```

* Define roles
```kotlin
sealed class DidRole : S2Role {
    class Admin : DidRole()
    class Owner : DidRole()

    override fun toString(): String {
        return this::class.simpleName!!
    }
}

```

* Define generics Command and Event
```kotlin
sealed interface DidInitCommand : S2InitCommand
sealed interface DidCommand : S2Command<DidId>
sealed interface DidEvent : S2Event<DidState, DidId>
```

* define object id
```kotlin
typealias DidId = String
```

* define automate
```kotlin
val didS2 = s2 {
	name = "DidS2"
	init<DidCreateCommand> {
		to = DidState.Created()
		role = DidRole.Admin()
	}
	transaction<DidAddPublicKeyCommand> {
		from = DidState.Created()
		to = DidState.Activated()
		role = DidRole.Owner()
	}
	transaction<DidRevokeCommand> {
		from = DidState.Activated()
		to = DidState.Activated()
		role = DidRole.Owner()
	}
	transaction<DidRevokePublicKeyCommand> {
		from = DidState.Activated()
		to = DidState.Revoked()
		role = DidRole.Owner()
	}
}
```

* Define transactions :
```kotlin
typealias DidCreateCommandFunction = F2Function<DidCreateCommand, DidCreatedEvent>

@Serializable
@JsExport
@JsName("DidCreateCommand")
data class DidCreateCommand(
	val id: DidId,
) : DidInitCommand

@Serializable
@JsExport
@JsName("DidCreatedEvent")
data class DidCreatedEvent(
	override val id: String,
	override val type: DidState,
) : DidEvent
```

```kotlin
typealias DidAddPublicKeyCommandFunction = F2Function<DidAddPublicKeyCommand, DidAddPublicKeyEvent>

class DidAddPublicKeyCommandPayload(
    val id: DidId,
)

data class DidAddPublicKeyCommand(
    override val id: DidId,
    val publicKey: String,
) : DidCommand

data class DidAddPublicKeyEvent(
    override val id: DidId,
    override val type: DidState,
) : DidEvent

```

```kotlin
typealias DidRevokeCommandFunction = F2Function<DidRevokeCommand, DidRevokedEvent>

data class DidRevokeCommand(
	override val id: DidId,
) : DidCommand, DidRevokeCommandPayload

data class DidRevokedEvent(
	override val id: DidId,
	override val type: DidState,
) : DidEvent
```

```kotlin
typealias DidRevokeCommandFunction = F2Function<DidRevokeCommand, DidRevokedEvent>

data class DidRevokeCommand(
    override val id: DidId,
) : DidCommand

data class DidRevokedEvent(
    override val id: DidId,
    override val type: DidState,
) : DidEvent
```

```kotlin
typealias DidRevokePublicKeyCommandFunction = F2Function<DidRevokePublicKeyCommand, DidRevokedPublicKeyEvent>

class DidRevokePublicKeyCommand(
	override val id: DidId,
) : DidCommand

class DidRevokedPublicKeyEvent(
	override val type: DidState,
	override val id: DidId,
) : DidEvent
```

### Configure storage and Implement transaction

#### State Storing

* Spring Data
  TODO
* Ssm
  TODO



#### Event sourcing
* Spring Data
  TODO
* Ssm
  TODO

## Concept

### Kotlin Multiplatform
Support for multiplatform programming is one of Kotlin’s key benefits. It reduces time spent writing and maintaining
the same code for different platforms while retaining the flexibility and benefits of native programming.

### Finite State Machine

A finite state machine (FSM) is a mathematical model used to describe a system that can exist
in one of a finite set of states. FSMs are composed of a set of states, a set of inputs, and a set of transitions
that describe how the machine moves from one state to another based on the inputs it receives.

In essence, FSMs are like decision-making models that take in inputs and use them to make a transition to a new state.
The behavior of an FSM is determined by its current state and the input it receives, and the machine transitions
to a new state based on a set of rules defined by its transitions.

Transitions can be either conditional, meaning they depend on the input or current state, or unconditional,
meaning they always occur regardless of the input or current state.

### Command Query Responsibility Segregation (CQRS)

Finite state machines (FSMs) can be useful in Command Query Responsibility Segregation (CQRS)
because they can be used to model the state transitions of a system.

Finite state machines (FSMs) can be used in conjunction with Command Query Responsibility Segregation (CQRS)
to model and manage the state of aggregates and entities.

CQRS is an architectural pattern that separates the operations that read data from those that write data.
This separation can improve scalability and performance, as read and write operations can be optimized independently.

FSMs can be used to model the state transitions of aggregates and entities, while CQRS can be used to separate
the read and write operations for those aggregates and entities. For example, write operations can be implemented
using commands that modify the state of an aggregate, while read operations can be implemented using queries
that retrieve data from the aggregate.

By combining FSMs and CQRS, developers can create a robust and scalable system for managing the state of aggregates
and entities. The FSM can ensure that the state transitions are valid and consistent, while CQRS can ensure
that read and write operations are optimized for performance and scalability.

Overall, using FSMs with CQRS can help developers create software systems that are reliable, scalable, and maintainable.
It can also help to ensure that the state of aggregates and entities is consistent with the business rules of the domain.

### Domain Drive Development

Finite state machines (FSMs) can be useful in Domain-Driven Design (DDD) because they can be used to describe root aggregates or entities.

In DDD, aggregates and entities are used to model complex business processes and transactions.
Aggregates are groups of related objects that are treated as a single transactional consistency boundary,
while entities are individual objects that have their own identity and lifecycle.

By using an FSM to model the state transitions of an aggregate or entity, developers can ensure that it remains
in a valid state throughout its lifecycle. This can be particularly useful when modeling complex business processes,
where the state of an object can impact the entire system.

In essence, using an FSM to model the state transitions of aggregates or entities can help developers ensure that
they behave correctly and consistently with the business rules of the domain. By doing so,
FSMs can contribute to the development of robust, reliable software systems that meet the needs of their users.


## Data management

Event-sourced and state-stored systems are two different approaches to managing the state of a software system.

### Event sourced

An event-sourced system is one that stores all changes to the system state as a series of events.
These events are typically immutable and can be used to reconstruct the system state at any point in time.
In an event-sourced system, state changes are represented as a series of events that are appended to an event log.
The current state of the system can then be derived by replaying the events in the log.

Event-sourced systems are often used in scenarios where it is important to maintain
a complete audit trail of all changes to the system state, or where the ability to reconstruct the system state
at any point in time is important.

### State stored

A state-stored system is one that directly stores the current state of the system in a database or other storage mechanism.
In a state-stored system, state changes are directly written to the database or storage mechanism,
and the current state of the system is read from the same storage mechanism.

State-stored systems, on the other hand, are often used in scenarios where it is more important to have fast read and write operations,
and where the ability to reconstruct the system state at any point in time is less important.


## Data Storage

### Spring Boot Data

Spring Boot Data is a module within the Spring Framework that simplifies the process of building Spring-powered applications that access data from a variety of data stores. It provides a set of tools for working with data, such as data access, manipulation, and transformation.

One of the key features of Spring Boot Data is its ability to provide a consistent and easy-to-use interface for interacting with different data sources, including relational databases, NoSQL databases, and in-memory data stores. This allows developers to write code that is more modular and portable, without worrying about the underlying implementation details of each data source.

### Ssm

Signing State Machines is a smart contract framework designed for Hyperledger Fabric blockchain platform and is implemented in Golang. This framework provides a more constrained programming paradigm for writing smart contracts based on finite state automata, which differs from traditional programming languages like Solidity.

In the Signing State Machines framework, every agent participating in the smart contract provides an identifier and a public key. The agent holds the private key corresponding to the public key stored in the smart contract and is never supposed to share it. The smart contract is deemed fulfilled when the Signing State Machine enters an acceptance state.

Each state transition in a Signing State Machine is a tuple of a role and an action. An agent performs a transition by signing the update with its private key. The smart contract validates the transaction by checking the state's signature with the public key of the agent assigned to the corresponding role, and then updates the state of the Signing State Machine.

