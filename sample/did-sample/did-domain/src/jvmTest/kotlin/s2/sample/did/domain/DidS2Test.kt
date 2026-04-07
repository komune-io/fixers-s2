package s2.sample.did.domain

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import s2.automate.documenter.S2Documenter
import s2.automate.documenter.writeS2Automate

class DidS2Test {

    @Test
    fun testS2JsonSerialization() {
        val json = Json.encodeToString(didS2)
        println(json)
    }
    @Test
    fun generateS2DocumentationDiagram() {
        S2Documenter()
            .writeS2Automate(didS2)
    }

}
