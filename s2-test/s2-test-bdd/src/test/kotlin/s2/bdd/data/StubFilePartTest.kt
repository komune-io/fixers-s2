package s2.bdd.data

import f2.dsl.cqrs.Event
import java.nio.file.Path
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import s2.bdd.TestApplicationEventListener

class StubFilePartTest {

    @Test
    fun `StubFilePart exposes its name as filename with empty content`() {
        val part = StubFilePart("report.pdf")
        assertThat(part.name()).isEqualTo("report.pdf")
        assertThat(part.filename()).isEqualTo("report.pdf")
        assertThat(part.headers().isEmpty).isTrue()
        val buffers = part.content().collectList().block()!!
        assertThat(buffers).hasSize(1)
        assertThat(buffers.single().readableByteCount()).isZero()
        assertThat(part.transferTo(Path.of("ignored")).blockOptional()).isEmpty()
    }

    private data class SomeEvent(val id: String) : Event

    @Test
    fun `TestApplicationEventListener stores application events in the context`() {
        val context = TestContext()
        val listener = TestApplicationEventListener(context)
        val event = SomeEvent("evt-1")
        listener.onApplicationEvent(event)
        assertThat(context.events).containsExactly(event)
    }
}
