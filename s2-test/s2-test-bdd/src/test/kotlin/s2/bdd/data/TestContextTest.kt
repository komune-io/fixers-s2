package s2.bdd.data

import f2.dsl.cqrs.Event
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import s2.bdd.auth.AuthedUser

class TestContextTest {

    private data class SomeEvent(val id: String) : Event

    @Test
    fun `testEntities should create and register a named entity list`() {
        val context = TestContext()

        val entities = context.testEntities<String, String>("order")

        assertThat(context.entityLists).containsEntry("order", entities)
    }

    @Test
    fun `reset should clear entities errors events and authed user`() {
        val context = TestContext()
        val entities = context.testEntities<String, String>("order")
        entities["a"] = "entityA"
        context.errors.add(IllegalStateException("boom"))
        context.events.add(SomeEvent("evt"))
        context.authedUser = AuthedUser(id = "user", memberOf = "org", roles = arrayOf("admin"))

        context.reset()

        assertThat(entities.items).isEmpty()
        assertThat(context.entityLists).containsKey("order")
        assertThat(context.errors.list).isEmpty()
        assertThat(context.events).isEmpty()
        assertThat(context.authedUser).isNull()
    }
}
