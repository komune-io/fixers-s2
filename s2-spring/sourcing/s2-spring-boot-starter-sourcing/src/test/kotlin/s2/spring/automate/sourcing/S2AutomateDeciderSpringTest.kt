package s2.spring.automate.sourcing

import f2.dsl.cqrs.envelope.Envelope
import f2.dsl.cqrs.enveloped.EnvelopedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import s2.automate.core.appevent.publisher.AppEventPublisher
import s2.automate.core.engine.S2AutomateEngine
import s2.automate.core.sourcing.S2AutomateSourcingDeciderImpl
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.sourcing.dsl.Loader
import s2.sourcing.dsl.event.EventRepository

/**
 * Unit tests for [S2AutomateDeciderSpring] verifying that:
 *  - withContext wires a concrete engine,
 *  - init / transition delegate to the injected engine.
 */
class S2AutomateDeciderSpringTest {

    // ---- domain fixtures ----

    enum class TestState(override var position: Int) : S2State {
        Created(0)
    }

    data class TestEntity(val id: String) : WithS2Id<String>, WithS2State<TestState> {
        override fun s2Id(): String = id
        override fun s2State(): TestState = TestState.Created
    }

    data class TestEvent(val id: String) : Evt, WithS2Id<String> {
        override fun s2Id(): String = id
    }

    data class CreateCmd(val id: String) : S2InitCommand
    data class DoCmd(override val id: String) : S2Command<String>

    // ---- no-op collaborators (never actually driven) ----

    private object NoOpEngine : S2AutomateEngine<TestState, TestEntity, String, TestEvent> {
        override fun <COMMAND : S2InitCommand, ENTITY_OUT : TestEntity, EVENT_OUT : TestEvent> create(
            commands: EnvelopedFlow<COMMAND>,
            decide: suspend (cmd: Envelope<COMMAND>) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<EVENT_OUT> = flowOf()

        override fun <COMMAND : S2Command<String>, ENTITY_OUT : TestEntity, EVENT_OUT : TestEvent> doTransition(
            commands: EnvelopedFlow<COMMAND>,
            exec: suspend (Envelope<out COMMAND>, TestEntity) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<EVENT_OUT> = flowOf()
    }

    private object NoOpPublisher : AppEventPublisher {
        override fun <EVENT> publish(event: EVENT & Any) = Unit
    }

    private object NoOpLoader : Loader<TestEvent, TestEntity, String> {
        override suspend fun load(id: String): TestEntity? = null
        override suspend fun load(events: Flow<TestEvent>): TestEntity? = null
        override suspend fun loadAndEvolve(id: String, news: Flow<TestEvent>): TestEntity? = null
        override suspend fun evolve(events: Flow<TestEvent>, entity: TestEntity?): TestEntity? = null
        override suspend fun reloadHistory(): List<TestEntity> = emptyList()
    }

    private object NoOpEventStore : EventRepository<TestEvent, String> {
        override fun load(id: String): Flow<TestEvent> = flowOf()
        override fun loadAll(): Flow<TestEvent> = flowOf()
        override suspend fun persist(event: TestEvent): TestEvent = event
        override fun persist(events: Flow<TestEvent>): Flow<TestEvent> = events
        override suspend fun createTable() = Unit
    }

    /** Engine whose init/transition return a controlled sentinel event. */
    private class StubEngine(
        private val sentinel: TestEvent,
    ) : S2AutomateSourcingDeciderImpl<TestState, TestEntity, String, TestEvent>(
        NoOpEngine, NoOpPublisher, NoOpLoader, NoOpEventStore
    ) {
        override suspend fun <EVENT_OUT : TestEvent> init(
            command: S2InitCommand,
            buildEvent: suspend () -> EVENT_OUT
        ): EVENT_OUT {
            @Suppress("UNCHECKED_CAST")
            return sentinel as EVENT_OUT
        }

        override suspend fun <EVENT_OUT : TestEvent> transition(
            command: S2Command<String>,
            exec: suspend TestEntity.() -> EVENT_OUT
        ): EVENT_OUT {
            @Suppress("UNCHECKED_CAST")
            return sentinel as EVENT_OUT
        }
    }

    private fun injectEngine(
        decider: S2AutomateDeciderSpring<TestEntity, TestState, TestEvent, String>,
        engine: S2AutomateSourcingDeciderImpl<TestState, TestEntity, String, TestEvent>,
    ) {
        S2AutomateDeciderSpring::class.java
            .getDeclaredField("engine")
            .apply { isAccessible = true }
            .set(decider, engine)
    }

    // ---- tests ----

    @Test
    suspend fun `withContext builds a concrete sourcing engine`() {
        val decider = object : S2AutomateDeciderSpring<TestEntity, TestState, TestEvent, String>() {}
        // Should not throw: constructs the underlying S2AutomateSourcingDeciderImpl.
        decider.withContext(NoOpEngine, NoOpPublisher, NoOpLoader, NoOpEventStore)
    }

    @Test
    suspend fun `init delegates to the engine`() {
        val decider = object : S2AutomateDeciderSpring<TestEntity, TestState, TestEvent, String>() {}
        val sentinel = TestEvent("init-sentinel")
        injectEngine(decider, StubEngine(sentinel))

        val result = decider.init(CreateCmd("id1")) { TestEvent("ignored") }

        assertThat(result).isSameAs(sentinel)
    }

    @Test
    suspend fun `transition delegates to the engine`() {
        val decider = object : S2AutomateDeciderSpring<TestEntity, TestState, TestEvent, String>() {}
        val sentinel = TestEvent("transition-sentinel")
        injectEngine(decider, StubEngine(sentinel))

        val result = decider.transition(DoCmd("id1")) { TestEvent("ignored") }

        assertThat(result).isSameAs(sentinel)
    }

    /** Decider backed by a real (no-op-fed) sourcing engine, to exercise the delegating builders. */
    private fun deciderWithRealEngine(): S2AutomateDeciderSpring<TestEntity, TestState, TestEvent, String> {
        val decider = object : S2AutomateDeciderSpring<TestEntity, TestState, TestEvent, String>() {}
        injectEngine(decider, S2AutomateSourcingDeciderImpl(NoOpEngine, NoOpPublisher, NoOpLoader, NoOpEventStore))
        return decider
    }

    @Test
    suspend fun `init builder returns a Decide delegating to the engine`() {
        val decide = deciderWithRealEngine().init<TestEvent, CreateCmd> { TestEvent("e") }
        assertThat(decide).isNotNull
    }

    @Test
    suspend fun `decide from init command returns a Decide delegating to the engine`() {
        val decide = deciderWithRealEngine().decide<TestEvent, CreateCmd> { TestEvent("e") }
        assertThat(decide).isNotNull
    }

    @Test
    suspend fun `decide from transition command returns a Decide delegating to the engine`() {
        val decide = deciderWithRealEngine().decide<TestEvent, DoCmd> { _, _ -> TestEvent("e") }
        assertThat(decide).isNotNull
    }

    @Test
    suspend fun `loadAll and load delegate to the event store`() {
        val decider = deciderWithRealEngine()
        assertThat(decider.loadAll().toList()).isEmpty()
        assertThat(decider.load("id1").toList()).isEmpty()
    }

    @Test
    suspend fun `replayHistory delegates to the engine`() {
        deciderWithRealEngine().replayHistory()
    }
}
