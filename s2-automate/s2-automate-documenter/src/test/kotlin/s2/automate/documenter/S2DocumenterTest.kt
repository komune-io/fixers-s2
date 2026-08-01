package s2.automate.documenter

import java.nio.file.Files
import java.nio.file.Path
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2Role
import s2.dsl.automate.S2State
import s2.dsl.automate.builder.s2

class S2DocumenterTest {

    enum class OrderState(override val position: Int) : S2State {
        Created(0)
    }

    enum class OrderRole(val value: String) : S2Role {
        Buyer("Buyer");

        override fun toString() = value
    }

    interface OrderCreateCommand : S2InitCommand

    private val automate = s2 {
        name = "OrderAutomate"
        init<OrderCreateCommand> {
            to = OrderState.Created
            role = OrderRole.Buyer
        }
    }

    @Test
    fun `recreateFile should create parent directories and an empty file`(@TempDir tempDir: Path) {
        val outputFolder = tempDir.resolve("nested/output").toString()

        val file = S2Documenter(outputFolder).recreateFile("automate.json", outputFolder)

        assertThat(file).exists()
        assertThat(Files.readAllBytes(file)).isEmpty()
    }

    @Test
    fun `recreateFile should replace an existing file`(@TempDir tempDir: Path) {
        val outputFolder = tempDir.toString()
        val documenter = S2Documenter(outputFolder)
        val existing = tempDir.resolve("automate.json")
        Files.writeString(existing, "old content")

        val file = documenter.recreateFile("automate.json", outputFolder)

        assertThat(file).exists()
        assertThat(Files.readString(file)).isEmpty()
    }

    @Test
    fun `writeS2Automate should write the automate as json named after the automate`(@TempDir tempDir: Path) {
        val documenter = S2Documenter(tempDir.toString())

        val result = documenter.writeS2Automate(automate)

        assertThat(result).isSameAs(documenter)
        val written = tempDir.resolve("OrderAutomate.json")
        assertThat(written).exists()
        val content = Files.readString(written)
        assertThat(content).contains("\"name\": \"OrderAutomate\"")
        assertThat(content).contains("OrderCreateCommand")
        assertThat(content).contains("OrderRole")
    }

    @Test
    fun `getDefaultOutputDirectory should point to gradle build folder when no pom exists`() {
        assertThat(getDefaultOutputDirectory()).isEqualTo("build/s2-documenter")
    }
}
