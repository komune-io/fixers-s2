package s2.automate.core.sourcing

import f2.dsl.cqrs.envelope.Envelope
import f2.dsl.cqrs.enveloped.EnvelopedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import s2.automate.core.appevent.publisher.AppEventPublisher
import s2.automate.core.engine.S2AutomateEngine
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.sourcing.dsl.Loader
import s2.sourcing.dsl.event.EventRepository

class S2AutomateSourcingDeciderImplTest {

    enum class TestState(override val position: Int) : S2State {
        Created(0), Active(1)
    }

    data class TestEntity(val id: String, val state: TestState = TestState.Created) :
        WithS2Id<String>, WithS2State<TestState> {
        override fun s2Id(): String = id
        override fun s2State(): TestState = state
    }

    sealed interface TestEvent : Evt, WithS2Id<String>
    data class CreatedEvt(val id: String) : TestEvent {
        override fun s2Id(): String = id
    }

    data class DoneEvt(val id: String) : TestEvent {
        override fun s2Id(): String = id
    }

    data class CreateCmd(val id: String) : S2InitCommand
    data class DoCmd(override val id: String) : S2Command<String>

    private class StubEngine : S2AutomateEngine<TestState, TestEntity, String, TestEvent> {
        override fun <COMMAND : S2InitCommand, ENTITY_OUT : TestEntity, EVENT_OUT : TestEvent> create(
            commands: EnvelopedFlow<COMMAND>,
            decide: suspend (cmd: Envelope<COMMAND>) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<EVENT_OUT> = commands.map { command -> decide(command).second }

        override fun <COMMAND : S2Command<String>, ENTITY_OUT : TestEntity, EVENT_OUT : TestEvent>
        doTransition(
            commands: EnvelopedFlow<COMMAND>,
            exec: suspend (Envelope<out COMMAND>, TestEntity) -> Pair<ENTITY_OUT, Envelope<EVENT_OUT>>
        ): EnvelopedFlow<EVENT_OUT> = commands.map { command ->
            exec(command, TestEntity(command.data.id)).second
        }
    }

    private class RecordingPublisher : AppEventPublisher {
        val published = mutableListOf<Any>()
        override fun <EVENT> publish(event: EVENT & Any) {
            published.add(event)
        }
    }

    private class StubLoader : Loader<TestEvent, TestEntity, String> {
        var reloadHistoryCalled = false
        override suspend fun load(id: String): TestEntity? = TestEntity(id)
        override suspend fun load(events: Flow<TestEvent>): TestEntity? = evolve(events)
        override suspend fun loadAndEvolve(id: String, news: Flow<TestEvent>): TestEntity? =
            evolve(news, TestEntity(id))

        override suspend fun evolve(events: Flow<TestEvent>, entity: TestEntity?): TestEntity? =
            events.fold(entity) { _, event -> TestEntity(event.s2Id()) }

        override suspend fun reloadHistory(): List<TestEntity> {
            reloadHistoryCalled = true
            return listOf(TestEntity("history"))
        }
    }

    private class StubEventRepository : EventRepository<TestEvent, String> {
        val stored = mutableListOf<TestEvent>(CreatedEvt("stored"))
        override fun load(id: String): Flow<TestEvent> =
            flowOf(*stored.filter { it.s2Id() == id }.toTypedArray())

        override fun loadAll(): Flow<TestEvent> = flowOf(*stored.toTypedArray())
        override suspend fun persist(event: TestEvent): TestEvent = event.also(stored::add)
        override fun persist(events: Flow<TestEvent>): Flow<TestEvent> = events
        override suspend fun createTable() { /* no-op for the stub */ }
    }

    private fun decider(
        publisher: RecordingPublisher = RecordingPublisher(),
        loader: StubLoader = StubLoader(),
        eventStore: StubEventRepository = StubEventRepository(),
    ) = S2AutomateSourcingDeciderImpl(StubEngine(), publisher, loader, eventStore)

    @Test
    suspend fun `init builds the event and publishes it`() {
        val publisher = RecordingPublisher()
        val decider = decider(publisher)
        val event = decider.init(CreateCmd("1")) { CreatedEvt("1") }
        assertEquals(CreatedEvt("1"), event)
        assertEquals(listOf<Any>(CreatedEvt("1")), publisher.published)
    }

    @Test
    suspend fun `transition executes against the loaded entity and publishes the event`() {
        val publisher = RecordingPublisher()
        val decider = decider(publisher)
        val event = decider.transition(DoCmd("7")) { DoneEvt(s2Id()) }
        assertEquals(DoneEvt("7"), event)
        assertEquals(listOf<Any>(DoneEvt("7")), publisher.published)
    }

    @Test
    suspend fun `decide flow overload maps every init command`() {
        val decider = decider()
        val events = decider.decide(flowOf(CreateCmd("1"), CreateCmd("2"))) { cmd -> CreatedEvt(cmd.id) }.toList()
        assertEquals(listOf(CreatedEvt("1"), CreatedEvt("2")), events)
    }

    @Test
    suspend fun `decide function overloads build reusable deciders`() {
        val decider = decider()
        val initDecide = decider.decide<CreatedEvt, CreateCmd> { cmd -> CreatedEvt(cmd.id) }
        val created = initDecide(flowOf(CreateCmd("a"))).first()
        assertEquals(CreatedEvt("a"), created)

        val transitionDecide = decider.decide<DoCmd, DoneEvt> { cmd, entity -> DoneEvt(entity.s2Id() + cmd.id) }
        val done = transitionDecide(flowOf(DoCmd("b"))).first()
        assertEquals(DoneEvt("bb"), done)
    }

    @Test
    suspend fun `loadAll and load delegate to the event store`() {
        val eventStore = StubEventRepository()
        val decider = decider(eventStore = eventStore)
        assertEquals(listOf<TestEvent>(CreatedEvt("stored")), decider.loadAll().toList())
        assertEquals(listOf<TestEvent>(CreatedEvt("stored")), decider.load("stored").toList())
        assertTrue(decider.load("missing").toList().isEmpty())
    }

    @Test
    suspend fun `replayHistory delegates to the projection loader`() {
        val loader = StubLoader()
        val decider = decider(loader = loader)
        decider.replayHistory()
        assertTrue(loader.reloadHistoryCalled)
    }
}
