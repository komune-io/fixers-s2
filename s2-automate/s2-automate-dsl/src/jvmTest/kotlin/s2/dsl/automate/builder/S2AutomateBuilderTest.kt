package s2.dsl.automate.builder

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2Role
import s2.dsl.automate.S2State

class S2AutomateBuilderTest {

    enum class OrderState(override val position: Int) : S2State {
        Created(0), Approved(1), Closed(2)
    }

    object Admin : S2Role

    data class CreateOrder(val value: String = "") : S2InitCommand
    data class ApproveOrder(override val id: String) : S2Command<String>
    data class UpdateOrder(override val id: String) : S2Command<String>
    data class CloseOrder(override val id: String) : S2Command<String>
    data class OrderCreated(val id: String) : Evt
    data class OrderApproved(val id: String) : Evt

    private val automate = s2 {
        name = "Order"
        version = "1.0.0"
        init<CreateOrder> {
            to = OrderState.Created
            role = Admin
            evt = OrderCreated::class
        }
        transaction<ApproveOrder> {
            from = OrderState.Created
            to = OrderState.Approved
            role = Admin
            evt = OrderApproved::class
        }
        selfTransaction<UpdateOrder> {
            states += OrderState.Created
            states += OrderState.Approved
            role = Admin
        }
        node {
            state = OrderState.Approved
            transaction<CloseOrder> {
                to = OrderState.Closed
                role = Admin
            }
        }
    }

    @Test
    fun `s2 builder registers name version and every transition flavor`() {
        assertThat(automate.name).isEqualTo("Order")
        assertThat(automate.version).isEqualTo("1.0.0")
        // init + transaction + selfTransaction(2 states) + node = 5
        assertThat(automate.transitions).hasSize(5)
    }

    @Test
    fun `init transition has no from state and carries the event`() {
        val init = automate.transitions.single { it.from == null }
        assertThat(init.action.name).isEqualTo(CreateOrder::class.simpleName)
        assertThat(init.result?.name).isEqualTo(OrderCreated::class.simpleName)
        assertThat(init.to.position).isEqualTo(OrderState.Created.position)
    }

    @Test
    fun `transaction with froms creates one transition per source state`() {
        val multi = s2 {
            name = "Multi"
            transaction<ApproveOrder> {
                froms += OrderState.Created
                froms += OrderState.Closed
                to = OrderState.Approved
                role = Admin
            }
        }
        assertThat(multi.transitions).hasSize(2)
        assertThat(multi.transitions.map { it.from?.position })
            .containsExactlyInAnyOrder(OrderState.Created.position, OrderState.Closed.position)
        assertThat(multi.version).isNull()
    }

    @Test
    fun `selfTransaction keeps the same from and to state`() {
        val selfs = automate.transitions.filter { it.action.name == UpdateOrder::class.simpleName }
        assertThat(selfs).hasSize(2)
        selfs.forEach { assertThat(it.from?.position).isEqualTo(it.to.position) }
    }

    @Test
    fun `node builder defaults to staying in the node state and supports explicit to`() {
        val close = automate.transitions.single { it.action.name == CloseOrder::class.simpleName }
        assertThat(close.from?.position).isEqualTo(OrderState.Approved.position)
        assertThat(close.to.position).isEqualTo(OrderState.Closed.position)

        val looping = s2 {
            name = "Loop"
            node {
                state = OrderState.Created
                transaction<UpdateOrder> {
                    role = Admin
                    evt = OrderApproved::class
                }
            }
        }
        val loop = looping.transitions.single()
        assertThat(loop.from?.position).isEqualTo(OrderState.Created.position)
        assertThat(loop.to.position).isEqualTo(OrderState.Created.position)
        assertThat(loop.result?.name).isEqualTo(OrderApproved::class.simpleName)
    }

    @Test
    fun `getAvailableTransitions returns transitions from the given state`() {
        val actions = automate.getAvailableTransitions(OrderState.Created).map { it.action.name }
        assertThat(actions).containsExactlyInAnyOrder(
            ApproveOrder::class.simpleName,
            UpdateOrder::class.simpleName,
        )
    }

    @Test
    fun `isAvailableTransition checks command against current state`() {
        assertThat(automate.isAvailableTransition(OrderState.Created, ApproveOrder("1"))).isTrue()
        assertThat(automate.isAvailableTransition(OrderState.Closed, ApproveOrder("1"))).isFalse()
    }

    @Test
    fun `isAvailableInitTransition only accepts init commands`() {
        assertThat(automate.isAvailableInitTransition(CreateOrder())).isTrue()
        assertThat(automate.isAvailableInitTransition(ApproveOrder("1"))).isFalse()
    }

    @Test
    fun `isFinalState is true when no transition leaves the state`() {
        assertThat(automate.isFinalState(OrderState.Closed)).isTrue()
        assertThat(automate.isFinalState(OrderState.Created)).isFalse()
    }

    @Test
    fun `isSameState compares positions and handles null`() {
        assertThat(automate.isSameState(OrderState.Created, OrderState.Created)).isTrue()
        assertThat(automate.isSameState(OrderState.Created, OrderState.Approved)).isFalse()
        assertThat(automate.isSameState(null, OrderState.Created)).isFalse()
    }

    @Test
    fun `withResultAsAction is true only when every transition carries a result`() {
        assertThat(automate.withResultAsAction).isFalse()
        val allResults = s2 {
            name = "AllResults"
            init<CreateOrder> {
                to = OrderState.Created
                role = Admin
                evt = OrderCreated::class
            }
        }
        assertThat(allResults.withResultAsAction).isTrue()
    }
}
