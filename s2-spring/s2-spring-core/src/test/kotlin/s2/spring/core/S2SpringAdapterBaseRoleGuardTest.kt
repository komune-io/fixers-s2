package s2.spring.core

import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextImpl
import reactor.core.publisher.Mono
import reactor.util.context.Context
import s2.automate.core.config.S2BatchProperties
import s2.automate.core.context.AutomateContext
import s2.automate.core.context.InitTransitionContext
import s2.automate.core.guard.Guard
import s2.automate.core.guard.RoleGuard
import s2.automate.core.guard.TransitionStateGuard
import s2.automate.core.guard.isValid
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2Role
import s2.dsl.automate.S2State
import s2.dsl.automate.builder.s2
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.spring.core.role.springSecurityRoles

class S2SpringAdapterBaseRoleGuardTest {

    enum class TestState(override val position: Int) : S2State {
        Created(0)
    }

    object Admin : S2Role

    data class TestEntity(val id: String) : WithS2Id<String>, WithS2State<TestState> {
        override fun s2Id(): String = id
        override fun s2State(): TestState = TestState.Created
    }

    data class CreateCmd(val id: String) : S2InitCommand

    class TestEvent : Evt

    private open class TestAdapter(
        private val isRoleValidated: Boolean,
    ) : S2SpringAdapterBase<TestEntity, TestState, TestEvent, String>() {
        override fun automate(): S2Automate = s2 {
            name = "RoleGuardAdapterTest"
            init<CreateCmd> {
                to = TestState.Created
                role = Admin
            }
        }

        override fun validateRoles(): Boolean = isRoleValidated

        fun exposedGuards(): List<Guard<TestState, String, TestEntity, TestEvent, S2Automate>> = guards()
    }

    @Test
    fun `roles are not validated by default`() {
        val guards = TestAdapter(isRoleValidated = false).exposedGuards()
        assertThat(guards).hasSize(1)
        assertThat(guards.single()).isInstanceOf(TransitionStateGuard::class.java)
    }

    @Test
    fun `the role guard is registered when the check is opted in`() {
        val guards = TestAdapter(isRoleValidated = true).exposedGuards()
        assertThat(guards).hasSize(2)
        assertThat(guards[0]).isInstanceOf(TransitionStateGuard::class.java)
        assertThat(guards[1]).isInstanceOf(RoleGuard::class.java)
    }

    // ---- default roles provider ----

    // Same shape as `s2-test-bdd`'s `CucumberStepsDefinition.authedContext()` injects.
    private suspend fun <T> withReactorSecurityRoles(vararg roles: String, block: suspend () -> T): T {
        val authentication = UsernamePasswordAuthenticationToken(
            "user",
            "pwd",
            roles.map { SimpleGrantedAuthority("ROLE_$it") },
        )
        val securityContext: SecurityContext = SecurityContextImpl(authentication)
        return withContext(
            ReactorContext(Context.of(SecurityContext::class.java, Mono.just(securityContext)))
        ) { block() }
    }

    @Test
    suspend fun `default roles provider reads the reactive security context and strips the ROLE prefix`() {
        val roles = withReactorSecurityRoles("Admin", "Owner") { springSecurityRoles() }
        // S2RoleValue has no structural equality, compare by name
        assertThat(roles.map { it.name }).containsExactlyInAnyOrder("Admin", "Owner")
    }

    @Test
    suspend fun `default roles provider returns nothing when no security context is present`() {
        assertThat(springSecurityRoles()).isEmpty()
    }

    @Test
    suspend fun `the registered guard accepts a caller holding the declared role`() {
        val guard = TestAdapter(isRoleValidated = true).exposedGuards()[1]
        val context = InitTransitionContext(
            automateContext = AutomateContext(
                automate = TestAdapter(isRoleValidated = true).automate(),
                batch = S2BatchProperties(),
            ),
            msg = CreateCmd("1"),
        )
        assertThat(withReactorSecurityRoles("Admin") { guard.evaluateInit(context) }.isValid()).isTrue()
        assertThat(withReactorSecurityRoles("Nope") { guard.evaluateInit(context) }.isValid()).isFalse()
    }
}
