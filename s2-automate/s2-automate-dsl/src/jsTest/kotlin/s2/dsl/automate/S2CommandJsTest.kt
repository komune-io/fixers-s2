package s2.dsl.automate

import kotlin.test.Test
import kotlin.test.assertEquals
import s2.dsl.automate.model.WithS2Id

class S2CommandJsTest {

	private data class CloseTicket(override val id: String) : S2Command<String>

	private data class Ticket(val id: String) : WithS2Id<String> {
		override fun s2Id(): String = id
	}

	@Test
	fun commandIdReadsTheIdOfAJsonPayload() {
		val command: S2Command<String> = JSON.parse<dynamic>("""{"id":"c1"}""").unsafeCast<S2Command<String>>()
		assertEquals("c1", command.id)
	}

	@Test
	fun commandIdIsExposedToJsUnderItsExportedName() {
		val command: S2Command<String> = CloseTicket("c2")
		assertEquals("c2", command.asDynamic().id)
	}

	@Test
	fun s2IdIsCallableFromJsUnderItsExportedName() {
		val ticket: WithS2Id<String> = Ticket("t1")
		assertEquals("t1", ticket.asDynamic().s2Id())
	}
}
