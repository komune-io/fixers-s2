package s2.dsl.automate.extention

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2Role
import s2.dsl.automate.S2State
import s2.dsl.automate.builder.s2
import s2.dsl.automate.model.WithS2State

class S2AutomateExtentionTest {

    enum class OrderState(override val position: Int) : S2State {
        Created(0), Approved(1)
    }

    object Admin : S2Role

    data class CreateOrder(val value: String = "") : S2InitCommand
    data class ApproveOrder(override val id: String) : S2Command<String>
    data class OrderApproved(val id: String) : Evt

    data class OrderEntity(val state: OrderState) : WithS2State<OrderState> {
        override fun s2State(): OrderState = state
    }

    private val automate = s2 {
        name = "Order"
        init<CreateOrder> {
            to = OrderState.Created
            role = Admin
        }
        transaction<ApproveOrder> {
            from = OrderState.Created
            to = OrderState.Approved
            role = Admin
            evt = OrderApproved::class
        }
    }

    @Test
    fun `isAvailableTransition matches by action name`() {
        assertThat(automate.isAvailableTransition(OrderState.Created, ApproveOrder::class.simpleName!!)).isTrue()
        assertThat(automate.isAvailableTransition(OrderState.Approved, ApproveOrder::class.simpleName!!)).isFalse()
    }

    @Test
    fun `isAvailableTransition matches by result event name`() {
        assertThat(automate.isAvailableTransition(OrderState.Created, OrderApproved::class.simpleName!!)).isTrue()
    }

    @Test
    fun `isAvailableTransition on model handles null model`() {
        val model: WithS2State<OrderState>? = null
        assertThat(automate.isAvailableTransition(model, ApproveOrder::class.simpleName!!)).isFalse()
        assertThat(
            automate.isAvailableTransition(OrderEntity(OrderState.Created), ApproveOrder::class.simpleName!!)
        ).isTrue()
    }

    @Test
    fun `canExecuteTransitionAnd combines transition availability and access`() {
        val entity = OrderEntity(OrderState.Created)
        assertThat(automate.canExecuteTransitionAnd<ApproveOrder>(entity) { true }).isTrue()
        assertThat(automate.canExecuteTransitionAnd<ApproveOrder>(entity) { false }).isFalse()
        val approved = OrderEntity(OrderState.Approved)
        assertThat(automate.canExecuteTransitionAnd<ApproveOrder>(approved) { true }).isFalse()
    }
}
