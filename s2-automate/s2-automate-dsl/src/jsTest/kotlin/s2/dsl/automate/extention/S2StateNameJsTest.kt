package s2.dsl.automate.extention

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class S2StateNameJsTest {

	private val automate = s2TicketAutomate()

	@Test
	fun stateNameReadsTheStatusOfAJsonPayload() {
		val payload: dynamic = JSON.parse("""{"id":"t1","status":"Open"}""")
		assertEquals("Open", s2StateName(payload.status))
		assertTrue(automate.isAvailableTransitionByName(s2StateName(payload.status), "CloseTicket"))
	}

	@Test
	fun stateNameReadsANameEnvelope() {
		val envelope: dynamic = JSON.parse("""{"name":"Closed","ordinal":1,"position":1}""")
		assertEquals("Closed", s2StateName(envelope))
		assertFalse(automate.isAvailableTransitionByName(s2StateName(envelope), "CloseTicket"))
	}

	@Test
	fun stateNameRefusesAnObjectWithoutAName() {
		val other: dynamic = JSON.parse("""{"position":0}""")
		assertNull(s2StateName(other))
	}

	@Test
	fun stateNameRefusesAnObjectThatIsNotAnEnumCopy() {
		val bare: dynamic = JSON.parse("""{"name":"Closed"}""")
		assertNull(s2StateName(bare))
		assertNull(s2StateName(Error("Closed")))
	}

	@Test
	fun stateNameRefusesTheKeyShape() {
		val keyed: dynamic = JSON.parse("""{"key":"Closed"}""")
		assertNull(s2StateName(keyed))
	}
}
