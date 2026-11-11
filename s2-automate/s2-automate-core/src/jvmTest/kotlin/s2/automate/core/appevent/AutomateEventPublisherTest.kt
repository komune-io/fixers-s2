package s2.automate.core.appevent

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import s2.automate.core.appevent.listener.AutomateListener
import s2.automate.core.appevent.listener.AutomateListenerAdapter
import s2.automate.core.appevent.publisher.AppEventPublisher
import s2.automate.core.appevent.publisher.AutomateEventPublisher
import s2.automate.core.persist.AutomatePersistFailure
import s2.dsl.automate.ErrorCategory
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.dsl.automate.s2error

class AutomateEventPublisherTest {

    enum class TestState(override val position: Int) : S2State {
        Created(0), Active(1)
    }

    data class TestEntity(val id: String, val state: TestState) : WithS2Id<String>, WithS2State<TestState> {
        override fun s2Id(): String = id
        override fun s2State(): TestState = state
    }

    data class CreateCmd(val id: String) : S2InitCommand

    private class RecordingPublisher : AppEventPublisher {
        val published = mutableListOf<Any>()
        override fun publish(event: Any) {
            published.add(event)
        }
    }

    private val automate = S2Automate(name = "Test", version = null, transitions = emptyArray())
    private val entity = TestEntity("1", TestState.Created)
    private val cmd = CreateCmd("1")

    private fun sendOneEventThroughEachCallback(
        listener: AutomateListener<TestState, String, TestEntity, S2Automate>,
    ): List<AppEvent> =
        listOf(
            AutomateStateEntered(TestState.Created).also(listener::automateStateEntered),
            AutomateStateExited(TestState.Created).also(listener::automateStateExited),
            AutomateTransitionNotAccepted(TestState.Created, cmd).also(listener::automateTransitionNotAccepted),
            AutomateInitTransitionStarted(cmd).also(listener::automateInitTransitionStarted),
            AutomateInitTransitionEnded(TestState.Created, cmd, entity).also(listener::automateInitTransitionEnded),
            AutomateTransitionStarted(TestState.Created, cmd).also(listener::automateTransitionStarted),
            AutomateTransitionEnded(from = TestState.Created, to = TestState.Active, msg = cmd, entity = entity)
                .also(listener::automateTransitionEnded),
            AutomateTransitionError(cmd, IllegalStateException("boom")).also(listener::automateTransitionError),
            AutomateSessionStarted(automate).also(listener::automateSessionStarted),
            AutomateSessionStopped(automate).also(listener::automateSessionStopped),
            AutomateSessionError(automate, IllegalStateException("boom")).also(listener::automateSessionError),
            AutomatePersistFailure("msg-1", ErrorCategory.Transient, s2error(code = "ERR", description = "desc"))
                .also(listener::automatePersistFailure),
        )

    @Test
    fun `AutomateEventPublisher forwards every listener callback to the publisher`() {
        val publisher = RecordingPublisher()
        val automatePublisher = AutomateEventPublisher<TestState, String, TestEntity, S2Automate>(publisher)
        val events = sendOneEventThroughEachCallback(automatePublisher)
        assertEquals(events.size, publisher.published.size)
        events.zip(publisher.published).forEach { (sent, received) ->
            assertSame(sent, received)
        }
    }

    @Test
    fun `AutomateListenerAdapter ignores every event by default`() {
        val adapter = AutomateListenerAdapter<TestState, String, TestEntity, S2Automate>()
        // Must not throw: every callback is an explicit no-op.
        assertEquals(12, sendOneEventThroughEachCallback(adapter).size)
    }

    @Test
    fun `app events expose their payload`() {
        val notAccepted = AutomateTransitionNotAccepted(TestState.Created, cmd)
        assertEquals(TestState.Created, notAccepted.from)
        assertEquals(cmd, notAccepted.msg)

        val initEnded = AutomateInitTransitionEnded(TestState.Created, cmd, entity)
        assertEquals(TestState.Created, initEnded.to)
        assertEquals(entity, initEnded.entity)

        val ended = AutomateTransitionEnded(from = TestState.Created, to = TestState.Active, msg = cmd, entity = entity)
        assertEquals(TestState.Created, ended.from)
        assertEquals(TestState.Active, ended.to)

        val boom = IllegalStateException("boom")
        val sessionError = AutomateSessionError(automate, boom)
        assertEquals(automate, sessionError.automate)
        assertSame(boom, sessionError.exception)
    }
}
