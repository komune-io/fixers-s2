package s2.automate.core.engine

import f2.dsl.cqrs.envelope.asEnvelopeWithType
import f2.dsl.fnc.operators.mapToEnvelope
import f2.dsl.fnc.operators.mapToEnvelopeWithRandomId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import s2.automate.core.appevent.publisher.AppEventPublisher
import s2.automate.core.appevent.publisher.AutomateEventPublisher
import s2.automate.core.config.S2BatchProperties
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.InitTransitionContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.context.TransitionContext
import s2.automate.core.error.AutomateException
import s2.automate.core.guard.GuardVerifier
import s2.automate.core.persist.AutomatePersister
import s2.automate.core.persist.PersistOutcome
import s2.dsl.automate.Cmd
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2Role
import s2.dsl.automate.S2State
import s2.dsl.automate.builder.s2
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Two commands targeting the same entity inside a single batch used to be collapsed:
 * `associateBy { it.data.id }` kept only the last one, and the earlier commands produced
 * neither an event nor an error. A batch loads each entity exactly once, so it cannot order
 * same-id commands against each other — they are now rejected explicitly.
 */
class S2AutomateEngineDuplicateIdsTest {

    enum class TestState(override val position: Int) : S2State {
        Created(0), Active(1)
    }

    object TestRole : S2Role

    data class TestEntity(val id: String, val state: TestState) : WithS2Id<String>, WithS2State<TestState> {
        override fun s2Id(): String = id
        override fun s2State(): TestState = state
    }

    data class CreateCmd(val id: String) : S2InitCommand
    data class DoCmd(override val id: String) : S2Command<String>

    sealed interface TestEvent { val entityId: String }
    data class DoneEvt(override val entityId: String) : TestEvent

    private val automate: S2Automate = s2 {
        name = "DuplicateIdsTest"
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

    /**
     * Behaves like a real repository: `findAllById` returns one row per *distinct* id,
     * which is exactly why duplicate commands used to vanish.
     */
    private class DedupingPersister(
        private val entities: Map<String, TestEntity>,
    ) : AutomatePersister<TestState, String, TestEntity, TestEvent, S2Automate> {

        val persistedIds = mutableListOf<String>()

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            id: String,
        ): TestEntity? = entities[id]

        override suspend fun load(
            automateContexts: AutomateContext<S2Automate>,
            ids: Flow<String>,
        ): Flow<TestEntity?> = ids.toList().distinct().mapNotNull { entities[it] }.asFlow()

        override suspend fun persistInit(
            transitionContexts: Flow<InitTransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<TestEvent> = transitionContexts.map { it.event }

        override suspend fun persist(
            transitionContexts: Flow<TransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>>
        ): Flow<TestEvent> = transitionContexts.map {
            persistedIds.add(it.entity.id)
            it.event
        }
    }

    private class PassthroughGuardVerifier :
        GuardVerifier<TestState, String, TestEntity, TestEvent, S2Automate> {
        override suspend fun evaluateInit(context: InitTransitionContext<S2Automate>) = Unit
        override suspend fun <COMMAND : Cmd> evaluateTransition(
            context: TransitionContext<TestState, String, TestEntity, S2Automate, COMMAND>
        ) = Unit

        override suspend fun verifyInitTransition(
            context: InitTransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>
        ) = context

        override suspend fun verifyTransition(
            context: TransitionAppliedContext<TestState, String, TestEntity, TestEvent, S2Automate>
        ) = context
    }

    private class NoopPublisher : AppEventPublisher {
        override fun <EVENT> publish(event: EVENT & Any) = Unit
    }

    private val entities = mapOf(
        "1" to TestEntity("1", TestState.Created),
        "2" to TestEntity("2", TestState.Created),
    )

    private fun automateContext(batchSize: Int) =
        AutomateContext(automate, S2BatchProperties(size = batchSize, concurrency = 1))

    private fun engine(
        persister: DedupingPersister,
        batchSize: Int = 10,
    ) = S2AutomateEngineImpl(
        automateContext(batchSize),
        PassthroughGuardVerifier(),
        persister,
        AutomateEventPublisher<TestState, String, TestEntity, S2Automate>(NoopPublisher()),
    )

    private fun outcomeEngine(
        persister: DedupingPersister,
        batchSize: Int = 10,
    ) = S2AutomateOutcomeEngineImpl(
        automateContext(batchSize),
        PassthroughGuardVerifier(),
        persister,
        AutomateEventPublisher<TestState, String, TestEntity, S2Automate>(NoopPublisher()),
    )

    // ---- doTransition: whole batch rejected ----

    @Test
    fun `doTransition rejects a batch holding two commands for the same entity`() = runTest {
        val persister = DedupingPersister(entities)
        val commands = flowOf(DoCmd("1"), DoCmd("1")).mapToEnvelopeWithRandomId(type = "Cmd")

        val exception = kotlin.runCatching {
            engine(persister).doTransition(commands) { cmd, entity ->
                TestEntity(entity.id, TestState.Active) to DoneEvt(cmd.data.id).asEnvelopeWithType("Evt")
            }.toList()
        }.exceptionOrNull()

        assertIs<AutomateException>(exception)
        assertEquals("ERROR_DUPLICATE_COMMAND_IDS", exception.errors.single().type)
        assertTrue(exception.errors.single().payload["ids"]!!.contains("1"))
        // nothing was persisted: the batch is refused as a whole
        assertEquals(emptyList(), persister.persistedIds)
    }

    @Test
    fun `doTransition reports every duplicated id, not just the first`() = runTest {
        val persister = DedupingPersister(entities)
        val commands = flowOf(DoCmd("1"), DoCmd("2"), DoCmd("1"), DoCmd("2"))
            .mapToEnvelopeWithRandomId(type = "Cmd")

        val exception = kotlin.runCatching {
            engine(persister).doTransition(commands) { cmd, entity ->
                TestEntity(entity.id, TestState.Active) to DoneEvt(cmd.data.id).asEnvelopeWithType("Evt")
            }.toList()
        }.exceptionOrNull()

        assertIs<AutomateException>(exception)
        assertEquals("1, 2", exception.errors.single().payload["ids"])
    }

    @Test
    fun `doTransition accepts distinct ids in the same batch`() = runTest {
        val persister = DedupingPersister(entities)
        val commands = flowOf(DoCmd("1"), DoCmd("2")).mapToEnvelopeWithRandomId(type = "Cmd")

        val events = engine(persister).doTransition(commands) { cmd, entity ->
            TestEntity(entity.id, TestState.Active) to DoneEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList()

        assertEquals(listOf("1", "2"), events.map { it.data.entityId })
    }

    @Test
    fun `doTransition still accepts the same id in two different batches`() = runTest {
        val persister = DedupingPersister(entities)
        // batch size 1 puts each command in its own batch, which reloads the entity
        val commands = flowOf(DoCmd("1"), DoCmd("1")).mapToEnvelopeWithRandomId(type = "Cmd")

        val events = engine(persister, batchSize = 1).doTransition(commands) { cmd, entity ->
            TestEntity(entity.id, TestState.Active) to DoneEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList()

        assertEquals(listOf("1", "1"), events.map { it.data.entityId })
    }

    // ---- doTransitionWithOutcomes: per-command rejection ----

    @Test
    fun `doTransitionWithOutcomes rejects the duplicate and keeps the first command`() = runTest {
        val persister = DedupingPersister(entities)
        val commands = flowOf(DoCmd("1"), DoCmd("1"), DoCmd("2"))
            .mapToEnvelope(type = "Cmd") { "msg-${it.id}-${it.hashCode()}" }

        val outcomes = outcomeEngine(persister).doTransitionWithOutcomes(commands) { cmd, entity ->
            TestEntity(entity.id, TestState.Active) to DoneEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList().map { it.data }

        val successes = outcomes.filterIsInstance<PersistOutcome.Success<DoneEvt>>()
        val rejected = outcomes.filterIsInstance<PersistOutcome.Rejected<DoneEvt>>()

        assertEquals(listOf("1", "2"), successes.map { it.event.entityId }.sorted())
        assertEquals(1, rejected.size)
        assertEquals("ERROR_DUPLICATE_COMMAND_IDS", rejected.single().error.type)
        // the winner is persisted exactly once, the loser never reaches persist
        assertEquals(listOf("1", "2"), persister.persistedIds.sorted())
    }

    @Test
    fun `doTransitionWithOutcomes leaves distinct ids untouched`() = runTest {
        val persister = DedupingPersister(entities)
        val commands = flowOf(DoCmd("1"), DoCmd("2"))
            .mapToEnvelope(type = "Cmd") { "msg-${it.id}" }

        val outcomes = outcomeEngine(persister).doTransitionWithOutcomes(commands) { cmd, entity ->
            TestEntity(entity.id, TestState.Active) to DoneEvt(cmd.data.id).asEnvelopeWithType("Evt")
        }.toList().map { it.data }

        assertEquals(2, outcomes.filterIsInstance<PersistOutcome.Success<DoneEvt>>().size)
        assertTrue(outcomes.none { it is PersistOutcome.Rejected })
    }
}
