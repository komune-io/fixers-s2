package s2.dsl.automate.extention

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2Role
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
		assertEquals("Open", TicketState.Open.toValue().name)
		assertEquals(listOf("Open", "Closed"), automate.transitions.map { it.to.name })
	}

	@Test
	fun automateRecordsTheNameOfEachObjectState() {
		assertEquals("Ajar", DoorState.Ajar.toValue().name)
	}

	@Test
	fun stateNameReadsTheWireStringAndTheState() {
		assertEquals("Open", s2StateName("Open"))
		assertEquals("Closed", s2StateName(TicketState.Closed))
		assertEquals("Shut", s2StateName(DoorState.Shut))
		assertNull(s2StateName(null))
		assertNull(s2StateName(42))
	}

	@Test
	fun transitionIsAvailableFromTheStateName() {
		assertTrue(automate.isAvailableTransitionByName("Open", CloseTicket::class.simpleName!!))
		assertTrue(automate.isAvailableTransitionByName("Open", TicketClosed::class.simpleName!!))
		assertFalse(automate.isAvailableTransitionByName("Closed", CloseTicket::class.simpleName!!))
		assertFalse(automate.isAvailableTransitionByName("Unknown", CloseTicket::class.simpleName!!))
		assertFalse(automate.isAvailableTransitionByName(null, CloseTicket::class.simpleName!!))
	}

	@Test
	fun canExecuteTransitionByNameCombinesAvailabilityAndAccess() {
		assertTrue(automate.canExecuteTransitionByNameAnd<CloseTicket>(s2StateName("Open")) { true })
		assertFalse(automate.canExecuteTransitionByNameAnd<CloseTicket>(s2StateName("Open")) { false })
		assertFalse(automate.canExecuteTransitionByNameAnd<CloseTicket>(s2StateName(TicketState.Closed)) { true })
	}
}
