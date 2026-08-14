package s2.automate.core.guard

import f2.dsl.cqrs.envelope.asEnvelopeWithType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import s2.automate.core.appevent.AutomateTransitionNotAccepted
import s2.automate.core.appevent.publisher.AppEventPublisher
import s2.automate.core.appevent.publisher.AutomateEventPublisher
import s2.automate.core.config.S2BatchProperties
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionAppliedContext
import s2.automate.core.context.InitTransitionContext
import s2.automate.core.context.TransitionAppliedContext
import s2.automate.core.context.TransitionContext
import s2.automate.core.error.AutomateException
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2Role
import s2.dsl.automate.S2State
import s2.dsl.automate.builder.s2
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.dsl.automate.s2error

class GuardTest {

    enum class TestState(override val position: Int) : S2State {
        Created(0), Active(1)
    }

    object TestRole : S2Role

    data class TestEntity(val id: String, val state: TestState) : WithS2Id<String>, WithS2State<TestState> {
        override fun s2Id(): String = id
        override fun s2State(): TestState = state
    }

    data class CreateCmd(val id: String) : S2InitCommand
    data class UndeclaredCreateCmd(val id: String) : S2InitCommand
    data class DoCmd(override val id: String) : S2Command<String>
    data class OtherCmd(override val id: String) : S2Command<String>

    private val automate: S2Automate = s2 {
        name = "GuardTest"
        init<CreateCmd> {
            to = TestState.Created
            role = TestRole
        }
        transaction<DoCmd> {
            from = TestState.Created
            to = TestState.Active
            role = TestRole
        }
    }

    private val automateContext = AutomateContext(automate, S2BatchProperties())

    private class RecordingPublisher : AppEventPublisher {
        val published = mutableListOf<Any>()
        override fun <EVENT> publish(event: EVENT & Any) {
            published.add(event)
        }
    }

    private fun automatePublisher(publisher: RecordingPublisher) =
        AutomateEventPublisher<TestState, String, TestEntity, S2Automate>(publisher)

    private fun transitionContext(entity: TestEntity, cmd: S2Command<String>) =
        TransitionContext<TestState, String, TestEntity, S2Automate, S2Command<String>>(
            automateContext = automateContext,
            from = entity.s2State(),
            command = cmd.asEnvelopeWithType("Cmd"),
            entity = entity,
        )

    // ---- GuardResult ----

    @Test
    fun `GuardResult valid has no errors`() {
        val result = GuardResult.valid()
        assertTrue(result.isValid())
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `GuardResult error from vararg and list is invalid`() {
        val error = s2error("E1", "first")
        assertFalse(GuardResult.error(error).isValid())
        val fromList = GuardResult.error(listOf(error, s2error("E2", "second")))
        assertFalse(fromList.isValid())
        assertEquals(2, fromList.errors.size)
    }

    // ---- GuardAdapter ----

    @Test
    suspend fun `GuardAdapter accepts everything by default`() {
        val adapter = GuardAdapter<TestState, String, TestEntity, String, S2Automate>()
        val entity = TestEntity("1", TestState.Created)
        assertTrue(adapter.evaluateInit(InitTransitionContext(automateContext, CreateCmd("1"))).isValid())
        assertTrue(adapter.evaluateTransition(transitionContext(entity, DoCmd("1"))).isValid())
        assertTrue(
            adapter.verifyInitTransition(
                InitTransitionAppliedContext(automateContext, "msg", CreateCmd("1"), "EVT", entity)
            ).isValid()
        )
        assertTrue(
            adapter.verifyTransition(
                TransitionAppliedContext(automateContext, "msg", TestState.Created, DoCmd("1"), "EVT", entity)
            ).isValid()
        )
    }

    // ---- TransitionStateGuard ----

    @Test
    suspend fun `TransitionStateGuard accepts an available transition`() {
        val guard = TransitionStateGuard<TestState, String, TestEntity, String, S2Automate>()
        val entity = TestEntity("1", TestState.Created)
        val result = guard.evaluateTransition(transitionContext(entity, DoCmd("1")))
        assertTrue(result.isValid())
    }

    @Test
    suspend fun `TransitionStateGuard rejects an unavailable transition`() {
        val guard = TransitionStateGuard<TestState, String, TestEntity, String, S2Automate>()
        val entity = TestEntity("1", TestState.Active)
        val result = guard.evaluateTransition(transitionContext(entity, DoCmd("1")))
        assertFalse(result.isValid())
        assertEquals("ERROR_INVALID_TRANSITION", result.errors.single().type)
    }

    // ---- InitTransitionStateGuard ----

    @Test
    suspend fun `InitTransitionStateGuard accepts a declared init command`() {
        val guard = InitTransitionStateGuard<TestState, String, TestEntity, String, S2Automate>()
        val result = guard.evaluateInit(InitTransitionContext(automateContext, CreateCmd("1")))
        assertTrue(result.isValid())
    }

    @Test
    suspend fun `InitTransitionStateGuard rejects an init command not declared by the automate`() {
        val guard = InitTransitionStateGuard<TestState, String, TestEntity, String, S2Automate>()
        val result = guard.evaluateInit(InitTransitionContext(automateContext, UndeclaredCreateCmd("1")))
        assertFalse(result.isValid())
        assertEquals("ERROR_INVALID_INIT_TRANSITION", result.errors.single().type)
    }

    @Test
    suspend fun `InitTransitionStateGuard rejects a transition command used as an init command`() {
        val guard = InitTransitionStateGuard<TestState, String, TestEntity, String, S2Automate>()
        val result = guard.evaluateInit(InitTransitionContext(automateContext, DoCmd("1")))
        assertFalse(result.isValid())
    }

    @Test
    suspend fun `InitTransitionStateGuard rejects a creation declared without an init transition`() {
        // Automates declaring their creation with `transaction<CMD> { to = ... }` and no `from`
        // register no transition at all, hence expose no init transition: enabling the guard on
        // such an automate rejects every creation. This is why the guard is opt-in.
        val looseAutomate = s2 {
            name = "GuardTestLoose"
            transaction<CreateCmd> {
                to = TestState.Created
                role = TestRole
            }
        }
        val guard = InitTransitionStateGuard<TestState, String, TestEntity, String, S2Automate>()
        val context = InitTransitionContext(
            AutomateContext(looseAutomate, S2BatchProperties()),
            CreateCmd("1"),
        )
        assertFalse(guard.evaluateInit(context).isValid())
    }

    // ---- GuardVerifierImpl ----

    private fun verifier(
        publisher: RecordingPublisher,
        guards: List<Guard<TestState, String, TestEntity, String, S2Automate>> = listOf(
            TransitionStateGuard(),
        ),
    ) = GuardVerifierImpl(guards, automatePublisher(publisher))

    @Test
    suspend fun `evaluateInit passes when all guards accept`() {
        val publisher = RecordingPublisher()
        verifier(publisher).evaluateInit(InitTransitionContext(automateContext, CreateCmd("1")))
        assertTrue(publisher.published.isEmpty())
    }

    @Test
    suspend fun `evaluateInit publishes not-accepted and throws for an undeclared init command`() {
        val publisher = RecordingPublisher()
        val exception = assertThrows<AutomateException> {
            verifier(publisher, listOf(InitTransitionStateGuard()))
                .evaluateInit(InitTransitionContext(automateContext, UndeclaredCreateCmd("1")))
        }
        assertEquals("ERROR_INVALID_INIT_TRANSITION", exception.errors.single().type)
        assertEquals(1, publisher.published.filterIsInstance<AutomateTransitionNotAccepted>().size)
    }

    @Test
    suspend fun `evaluateTransition passes for a valid transition`() {
        val publisher = RecordingPublisher()
        val entity = TestEntity("1", TestState.Created)
        verifier(publisher).evaluateTransition(transitionContext(entity, DoCmd("1")))
        assertTrue(publisher.published.isEmpty())
    }

    @Test
    suspend fun `evaluateTransition publishes not-accepted and throws for an invalid transition`() {
        val publisher = RecordingPublisher()
        val entity = TestEntity("1", TestState.Active)
        val exception = assertThrows<AutomateException> {
            verifier(publisher).evaluateTransition(transitionContext(entity, DoCmd("1")))
        }
        assertEquals("ERROR_INVALID_TRANSITION", exception.errors.single().type)
        val event = publisher.published.single() as AutomateTransitionNotAccepted
        assertEquals(TestState.Active, event.from)
    }

    @Test
    suspend fun `verifyInitTransition returns the context when valid`() {
        val publisher = RecordingPublisher()
        val entity = TestEntity("1", TestState.Created)
        val context = InitTransitionAppliedContext<TestState, String, TestEntity, String, S2Automate>(
            automateContext, "msg", CreateCmd("1"), "EVT", entity
        )
        val returned = verifier(publisher).verifyInitTransition(context)
        assertSame(context, returned)
    }

    @Test
    suspend fun `verifyTransition aggregates errors from all guards`() {
        val publisher = RecordingPublisher()
        val failingGuard = object : GuardAdapter<TestState, String, TestEntity, String, S2Automate>() {
            override suspend fun verifyTransition(
                context: TransitionAppliedContext<TestState, String, TestEntity, String, S2Automate>
            ) = GuardResult.error(s2error("E1", "one"), s2error("E2", "two"))
        }
        val entity = TestEntity("1", TestState.Created)
        val context = TransitionAppliedContext<TestState, String, TestEntity, String, S2Automate>(
            automateContext, "msg", TestState.Created, DoCmd("1"), "EVT", entity
        )
        val exception = assertThrows<AutomateException> {
            verifier(publisher, listOf(failingGuard)).verifyTransition(context)
        }
        assertEquals(listOf("E1", "E2"), exception.errors.map { it.type })
        assertEquals(1, publisher.published.size)
    }

    @Test
    suspend fun `verifyTransition returns the context when valid`() {
        val publisher = RecordingPublisher()
        val entity = TestEntity("1", TestState.Created)
        val context = TransitionAppliedContext<TestState, String, TestEntity, String, S2Automate>(
            automateContext, "msg", TestState.Created, DoCmd("1"), "EVT", entity
        )
        val returned = verifier(publisher).verifyTransition(context)
        assertSame(context, returned)
        assertTrue(publisher.published.isEmpty())
    }
}
