package s2.automate.core.appevent

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
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
        override fun <EVENT> publish(event: EVENT & Any) {
            published.add(event)
        }
    }

    private val automate = S2Automate(name = "Test", version = null, transitions = emptyArray())
    private val entity = TestEntity("1", TestState.Created)
    private val cmd = CreateCmd("1")

    private fun allEvents(): List<AppEvent> = listOf(
        AutomateStateExited(TestState.Created),
        AutomateTransitionNotAccepted(TestState.Created, cmd),
        AutomateInitTransitionStarted(cmd),
        AutomateTransitionStarted(TestState.Created, cmd),
        AutomateTransitionEnded(TestState.Created, TestState.Active, cmd, entity),
        AutomateTransitionError(cmd, IllegalStateException("boom")),
        AutomateSessionStopped(automate),
        AutomatePersistFailure("msg-1", ErrorCategory.Transient, s2error("ERR", "desc")),
    )

    private fun dispatch(
        listener: s2.automate.core.appevent.listener.AutomateListener<TestState, String, TestEntity, S2Automate>,
        events: List<AppEvent>,
    ) {
        events.forEach { event ->
            @Suppress("UNCHECKED_CAST")
            when (event) {
                is AutomateStateExited -> listener.automateStateExited(event)
                is AutomateTransitionNotAccepted -> listener.automateTransitionNotAccepted(event)
                is AutomateInitTransitionStarted -> listener.automateInitTransitionStarted(event)
                is AutomateTransitionStarted -> listener.automateTransitionStarted(event)
                is AutomateTransitionEnded<*, *> ->
                    listener.automateTransitionEnded(event as AutomateTransitionEnded<TestState, TestEntity>)
                is AutomateTransitionError -> listener.automateTransitionError(event)
                is AutomateSessionStopped<*> ->
                    listener.automateSessionStopped(event as AutomateSessionStopped<S2Automate>)
                is AutomatePersistFailure -> listener.automatePersistFailure(event)
                else -> error("Unhandled event $event")
            }
        }
    }

    @Test
    fun `AutomateEventPublisher forwards every listener callback to the publisher`() {
        val publisher = RecordingPublisher()
        val automatePublisher = AutomateEventPublisher<TestState, String, TestEntity, S2Automate>(publisher)
        val events = allEvents()
        dispatch(automatePublisher, events)
        assertEquals(events.size, publisher.published.size)
        events.zip(publisher.published).forEach { (sent, received) ->
            assertSame(sent, received)
        }
    }

    @Test
    fun `AutomateListenerAdapter ignores every event by default`() {
        val adapter = AutomateListenerAdapter<TestState, String, TestEntity, S2Automate>()
        // Must not throw: every callback is an explicit no-op.
        dispatch(adapter, allEvents())
        assertTrue(true)
    }

    @Test
    fun `app events expose their payload`() {
        val notAccepted = AutomateTransitionNotAccepted(TestState.Created, cmd)
        assertEquals(TestState.Created, notAccepted.from)
        assertEquals(cmd, notAccepted.msg)

        val ended = AutomateTransitionEnded(TestState.Created, TestState.Active, cmd, entity)
        assertEquals(TestState.Created, ended.from)
        assertEquals(TestState.Active, ended.to)
    }
}
