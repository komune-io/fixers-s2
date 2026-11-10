package s2.dsl.automate.extention

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2Role
import s2.dsl.automate.S2RoleValue
import s2.dsl.automate.S2StateValue
import s2.dsl.automate.S2Transition
import s2.dsl.automate.S2TransitionValue
import s2.dsl.automate.S2State
import s2.dsl.automate.builder.s2
import s2.dsl.automate.toValue

enum class TicketState(override val position: Int) : S2State {
    Open(0), Closed(1)
}

sealed class DoorState(override val position: Int) : S2State {
    object Shut : DoorState(0)
    object Ajar : DoorState(1)
}

object Clerk : S2Role

enum class DeskRole : S2Role {
    Reception,
    Manager {
        override fun toString() = "manager"
    }
}

class OpenTicket : S2InitCommand
data class CloseTicket(override val id: String) : S2Command<String>
data class TicketClosed(val id: String) : Evt

fun s2TicketAutomate() = s2 {
    name = "Ticket"
    init<OpenTicket> {
        to = TicketState.Open
        role = Clerk
    }
    transaction<CloseTicket> {
        from = TicketState.Open
        to = TicketState.Closed
        role = Clerk
        evt = TicketClosed::class
    }
}

class S2StateNameTest {

    private val automate = s2TicketAutomate()

    @Test
    fun automateRecordsTheNameOfEachEnumState() {
        assertEquals(expected = "Open", actual = TicketState.Open.toValue().name)
        assertEquals(expected = listOf("Open", "Closed"), actual = automate.transitions.map { it.to.name })
    }

    @Test
    fun automateRecordsTheNameOfEachObjectState() {
        assertEquals(expected = "Ajar", actual = DoorState.Ajar.toValue().name)
    }

    @Test
    fun stateNameReadsTheWireStringAndTheState() {
        assertEquals(expected = "Open", actual = s2StateName("Open"))
        assertEquals(expected = "Closed", actual = s2StateName(TicketState.Closed))
        assertEquals(expected = "Shut", actual = s2StateName(DoorState.Shut))
        assertNull(s2StateName(null))
        assertNull(s2StateName(42))
    }

    @Test
    fun transitionIsAvailableFromTheStateName() {
        assertTrue(automate.isAvailableTransitionByName(stateName = "Open", type = CloseTicket::class.simpleName!!))
        assertTrue(automate.isAvailableTransitionByName(stateName = "Open", type = TicketClosed::class.simpleName!!))
        assertFalse(automate.isAvailableTransitionByName(stateName = "Closed", type = CloseTicket::class.simpleName!!))
        assertFalse(automate.isAvailableTransitionByName(stateName = "Unknown", type = CloseTicket::class.simpleName!!))
        assertFalse(automate.isAvailableTransitionByName(stateName = null, type = CloseTicket::class.simpleName!!))
    }

    @Test
    fun canExecuteTransitionByNameCombinesAvailabilityAndAccess() {
        assertTrue(automate.canExecuteTransitionByNameAnd<CloseTicket>(s2StateName("Open")) { true })
        assertFalse(automate.canExecuteTransitionByNameAnd<CloseTicket>(s2StateName("Open")) { false })
        assertFalse(automate.canExecuteTransitionByNameAnd<CloseTicket>(s2StateName(TicketState.Closed)) { true })
    }

    @Test
    fun automateRecordsTheNameOfEachEnumRole() {
        assertEquals(expected = "Reception", actual = DeskRole.Reception.toValue().name)
        assertEquals(expected = "Manager", actual = DeskRole.Manager.toValue().name)
    }

    @Test
    fun automateRefusesTwoStatesSharingAName() {
        val role = S2RoleValue("Clerk")
        val close = S2TransitionValue("CloseTicket")
        assertFailsWith<IllegalArgumentException> {
            S2Automate(
                name = "Ticket",
                version = null,
                transitions = arrayOf(
                    S2Transition(
                        from = S2StateValue("TicketState", 0),
                        to = S2StateValue("TicketState", 1),
                        role = role,
                        action = close,
                        result = null,
                    ),
                ),
            )
        }
    }

    @Test
    fun automateRefusesTwoNamesSharingAPosition() {
        val role = S2RoleValue("Clerk")
        val close = S2TransitionValue("CloseTicket")
        assertFailsWith<IllegalArgumentException> {
            S2Automate(
                name = "Ticket",
                version = null,
                transitions = arrayOf(
                    S2Transition(
                        from = S2StateValue("Open", 0),
                        to = S2StateValue("Closed", 0),
                        role = role,
                        action = close,
                        result = null,
                    ),
                ),
            )
        }
    }
}
