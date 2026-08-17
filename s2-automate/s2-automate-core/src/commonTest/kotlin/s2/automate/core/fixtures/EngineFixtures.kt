package s2.automate.core.fixtures

import f2.dsl.fnc.operators.BATCH_DEFAULT_CONCURRENCY
import s2.automate.core.appevent.publisher.AppEventPublisher
import s2.automate.core.appevent.publisher.AutomateEventPublisher
import s2.automate.core.config.S2BatchProperties
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.InitTransitionContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.context.TransitionContext
import s2.automate.core.engine.S2AutomateEngineImpl
import s2.automate.core.engine.S2AutomateOutcomeEngineImpl
import s2.automate.core.guard.GuardVerifier
import s2.automate.core.persist.AutomatePersister
import s2.dsl.automate.Cmd
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2Role
import s2.dsl.automate.S2State
import s2.dsl.automate.builder.s2
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

/**
 * Shared domain fixtures for the engine/evolver tests: a two-state automate
 * (Created → Active), its commands/events, and passthrough stub doubles.
 *
 * Tests whose fixtures differ semantically (extra fields, other automate wiring,
 * counting doubles) keep their own local declarations.
 */

enum class TestState(override val position: Int) : S2State {
    Created(0), Active(1)
}

object TestRole : S2Role

data class TestEntity(
    val id: String,
    val state: TestState,
) : WithS2Id<String>, WithS2State<TestState> {
    override fun s2Id(): String = id
    override fun s2State(): TestState = state
}

data class CreateCmd(val id: String) : S2InitCommand
data class DoCmd(override val id: String) : S2Command<String>

sealed interface TestEvent : Evt {
    val entityId: String
}

data class CreatedEvt(override val entityId: String) : TestEvent
data class DoneEvt(override val entityId: String) : TestEvent

fun testAutomate(automateName: String = "TestAutomate"): S2Automate = s2 {
    name = automateName
    init<CreateCmd> {
        to = TestState.Created
        role = TestRole
    }
    transaction<DoCmd> {
        from = TestState.Created
        to = TestState.Active
        role = TestRole
    }
}

/** Guard that always passes — used for non-guard failure scenarios. */
class PassthroughGuard<EVENT> : GuardVerifier<TestState, String, TestEntity, EVENT, S2Automate> {

    override suspend fun evaluateInit(context: InitTransitionContext<S2Automate>) = Unit

    override suspend fun <COMMAND : Cmd> evaluateTransition(
        context: TransitionContext<TestState, String, TestEntity, S2Automate, COMMAND>
    ) = Unit

    override suspend fun verifyInitTransition(
        context: InitTransitionAppliedContext<TestState, String, TestEntity, EVENT, S2Automate>
    ): InitTransitionAppliedContext<TestState, String, TestEntity, EVENT, S2Automate> = context

    override suspend fun verifyTransition(
        context: TransitionAppliedContext<TestState, String, TestEntity, EVENT, S2Automate>
    ): TransitionAppliedContext<TestState, String, TestEntity, EVENT, S2Automate> = context
}

class NoopPublisher : AppEventPublisher {
    override fun <EVENT> publish(event: EVENT & Any) = Unit
}

fun <EVENT> makeEngine(
    persister: AutomatePersister<TestState, String, TestEntity, EVENT, S2Automate>,
    guard: GuardVerifier<TestState, String, TestEntity, EVENT, S2Automate> = PassthroughGuard(),
    publisher: AppEventPublisher = NoopPublisher(),
    automate: S2Automate = testAutomate(),
    batchSize: Int = 10,
    concurrency: Int = BATCH_DEFAULT_CONCURRENCY,
): S2AutomateEngineImpl<TestState, String, TestEntity, EVENT> = S2AutomateEngineImpl(
    AutomateContext(automate, S2BatchProperties(size = batchSize, concurrency = concurrency)),
    guard,
    persister,
    AutomateEventPublisher<TestState, String, TestEntity, S2Automate>(publisher),
)

fun <EVENT> makeOutcomeEngine(
    persister: AutomatePersister<TestState, String, TestEntity, EVENT, S2Automate>,
    guard: GuardVerifier<TestState, String, TestEntity, EVENT, S2Automate> = PassthroughGuard(),
    publisher: AppEventPublisher = NoopPublisher(),
    automate: S2Automate = testAutomate(),
    batchSize: Int = 10,
    concurrency: Int = BATCH_DEFAULT_CONCURRENCY,
): S2AutomateOutcomeEngineImpl<TestState, String, TestEntity, EVENT> = S2AutomateOutcomeEngineImpl(
    AutomateContext(automate, S2BatchProperties(size = batchSize, concurrency = concurrency)),
    guard,
    persister,
    AutomateEventPublisher<TestState, String, TestEntity, S2Automate>(publisher),
)
