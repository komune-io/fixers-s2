package s2.dsl.automate

import kotlin.test.Test
import kotlin.test.assertEquals

class S2CommandJsTest {

	@Test
	fun commandIdReadsTheIdOfAJsonPayload() {
		val command: S2Command<String> = JSON.parse<dynamic>("""{"id":"c1"}""").unsafeCast<S2Command<String>>()
		assertEquals("c1", command.id)
	}
}
