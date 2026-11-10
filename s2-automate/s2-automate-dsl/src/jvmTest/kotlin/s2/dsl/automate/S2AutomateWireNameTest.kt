package s2.dsl.automate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper

/** `withResultAsAction` is read by name in the JSON a Spring endpoint writes with Jackson. */
class S2AutomateWireNameTest {

    private val automate = S2Automate(name = "Probe", version = null, transitions = emptyArray())

    @Test
    fun `Jackson writes withResultAsAction`() {
        assertThat(jacksonObjectMapper().writeValueAsString(automate)).contains("\"withResultAsAction\":true")
    }
}
