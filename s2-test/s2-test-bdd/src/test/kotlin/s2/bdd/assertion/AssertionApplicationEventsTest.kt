package s2.bdd.assertion

import f2.dsl.cqrs.Event
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import s2.bdd.data.TestContext

class AssertionApplicationEventsTest {

    private data class OrderPlacedEvent(val orderId: String) : Event
    private data class OrderCanceledEvent(val orderId: String) : Event

    private fun contextWithEvents(vararg events: Event): TestContext {
        val context = TestContext()
        context.events.addAll(events)
        return context
    }

    @Test
    fun `hasBeenSent should pass when count matches`() {
        val context = contextWithEvents(
            OrderPlacedEvent("1"),
            OrderPlacedEvent("2"),
            OrderCanceledEvent("1")
        )

        AssertionBdd.events(context)
            .assertThat(OrderPlacedEvent::class)
            .hasBeenSent(2)
    }

    @Test
    fun `hasBeenSent should fail when count does not match`() {
        val context = contextWithEvents(OrderPlacedEvent("1"))

        assertThatThrownBy {
            AssertionBdd.events(context)
                .assertThat(OrderPlacedEvent::class)
                .hasBeenSent(2)
        }.isInstanceOf(AssertionError::class.java)
    }

    @Test
    fun `hasNotBeenSent should pass when no matching event`() {
        val context = contextWithEvents(OrderCanceledEvent("1"))

        AssertionBdd.events(context)
            .assertThat(OrderPlacedEvent::class)
            .hasNotBeenSent()
    }

    @Test
    fun `hasBeenSentAtLeast and atMost should respect bounds`() {
        val context = contextWithEvents(
            OrderPlacedEvent("1"),
            OrderPlacedEvent("2")
        )
        val assertion = AssertionBdd.events(context).assertThat(OrderPlacedEvent::class)

        assertion.hasBeenSentAtLeast(1)
        assertion.hasBeenSentAtMost(2)
        assertThatThrownBy { assertion.hasBeenSentAtLeast(3) }
            .isInstanceOf(AssertionError::class.java)
        assertThatThrownBy { assertion.hasBeenSentAtMost(1) }
            .isInstanceOf(AssertionError::class.java)
    }

    @Test
    fun `matcher should filter events`() {
        val context = contextWithEvents(
            OrderPlacedEvent("match"),
            OrderPlacedEvent("no-match")
        )

        AssertionBdd.events(context)
            .assertThat(OrderPlacedEvent::class)
            .hasBeenSent(1) { it.orderId == "match" }
    }
}
