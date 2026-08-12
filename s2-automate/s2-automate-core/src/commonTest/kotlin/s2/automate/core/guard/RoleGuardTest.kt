package s2.automate.core.guard

import f2.dsl.cqrs.envelope.asEnvelopeWithType
import kotlinx.coroutines.test.runTest
import s2.automate.core.config.S2BatchProperties
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionContext
import s2.automate.core.context.TransitionContext
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2Command
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2Role
import s2.dsl.automate.S2RoleValue
import s2.dsl.automate.S2State
import s2.dsl.automate.builder.s2
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RoleGuardTest {

    enum class TestState(override val position: Int) : S2State {
        Created(0), Active(1)
    }

    object Admin : S2Role
    object Owner : S2Role

    data class TestEntity(val id: String, val state: TestState) : WithS2Id<String>, WithS2State<TestState> {
        override fun s2Id(): String = id
        override fun s2State(): TestState = state
    }

    data class CreateCmd(val id: String) : S2InitCommand
    data class DoCmd(override val id: String) : S2Command<String>
    data class UndeclaredCmd(override val id: String) : S2Command<String>

    private val automate: S2Automate = s2 {
        name = "RoleGuardTest"
        init<CreateCmd> {
            to = TestState.Created
            role = Admin
        }
        transaction<DoCmd> {
            from = TestState.Created
            to = TestState.Active
            role = Owner
        }
    }

    private val automateContext = AutomateContext(automate, S2BatchProperties())

    private fun guard(vararg roles: String) =
        RoleGuard<TestState, String, TestEntity, String> { roles.map { S2RoleValue(it) }.toSet() }

    private fun initContext(cmd: S2InitCommand = CreateCmd("1")) =
        InitTransitionContext(automateContext, cmd)

    private fun transitionContext(cmd: S2Command<String> = DoCmd("1")) =
        TransitionContext(
            automateContext = automateContext,
            from = TestState.Created,
            command = cmd.asEnvelopeWithType(type = "Cmd"),
            entity = TestEntity("1", TestState.Created),
        )

    // ---- init transitions ----

    @Test
    fun `init transition accepted when the caller holds the declared role`() = runTest {
        assertTrue(guard("Admin").evaluateInit(initContext()).isValid())
    }

    @Test
    fun `init transition rejected when the caller holds another role`() = runTest {
        val result = guard("Owner").evaluateInit(initContext())
        assertFalse(result.isValid())
        assertEquals("ERROR_MISSING_ROLE", result.errors.single().type)
        assertEquals("Admin", result.errors.single().payload["requiredRoles"])
        assertEquals("Owner", result.errors.single().payload["actualRoles"])
    }

    @Test
    fun `init transition rejected for an unauthenticated caller`() = runTest {
        assertFalse(guard().evaluateInit(initContext()).isValid())
    }

    // ---- transitions ----

    @Test
    fun `transition accepted when the caller holds the declared role`() = runTest {
        assertTrue(guard("Owner").evaluateTransition(transitionContext()).isValid())
    }

    @Test
    fun `transition rejected when the caller only holds the init role`() = runTest {
        val result = guard("Admin").evaluateTransition(transitionContext())
        assertFalse(result.isValid())
        assertEquals("ERROR_MISSING_ROLE", result.errors.single().type)
    }

    @Test
    fun `one matching role among several is enough`() = runTest {
        assertTrue(guard("Something", "Owner", "Else").evaluateTransition(transitionContext()).isValid())
    }

    // ---- matching rules ----

    @Test
    fun `role names are matched case-insensitively`() = runTest {
        // identity providers commonly lowercase role names; S2 roles are class simple names
        assertTrue(guard("owner").evaluateTransition(transitionContext()).isValid())
        assertTrue(guard("ADMIN").evaluateInit(initContext()).isValid())
    }

    @Test
    fun `a command matching no declared transition is left to the state guards`() = runTest {
        // no role can be resolved for it, so RoleGuard must not invent a rejection
        assertTrue(guard().evaluateTransition(transitionContext(UndeclaredCmd("1"))).isValid())
    }

    @Test
    fun `the roles provider is only consulted when a role is actually declared`() = runTest {
        var calls = 0
        val guard = RoleGuard<TestState, String, TestEntity, String> {
            calls++
            emptySet()
        }
        guard.evaluateTransition(transitionContext(UndeclaredCmd("1")))
        assertEquals(0, calls)
        guard.evaluateTransition(transitionContext())
        assertEquals(1, calls)
    }

    @Test
    fun `the roles provider is consulted per command, not cached`() = runTest {
        val roles = mutableListOf(setOf(S2RoleValue("Nope")), setOf(S2RoleValue("Owner")))
        val guard = RoleGuard<TestState, String, TestEntity, String> { roles.removeFirst() }
        assertFalse(guard.evaluateTransition(transitionContext()).isValid())
        assertTrue(guard.evaluateTransition(transitionContext()).isValid())
    }
}
