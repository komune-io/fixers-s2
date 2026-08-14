package s2.spring.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import s2.automate.core.guard.Guard
import s2.automate.core.guard.InitTransitionStateGuard
import s2.automate.core.guard.TransitionStateGuard
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2InitCommand
import s2.dsl.automate.S2Role
import s2.dsl.automate.S2State
import s2.dsl.automate.builder.s2
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State

class S2SpringAdapterBaseTest {

	enum class TestState(override val position: Int) : S2State {
		Created(0)
	}

	object TestRole : S2Role

	data class TestEntity(val id: String) : WithS2Id<String>, WithS2State<TestState> {
		override fun s2Id(): String = id
		override fun s2State(): TestState = TestState.Created
	}

	data class CreateCmd(val id: String) : S2InitCommand

	class TestEvent : Evt

	private open class TestAdapter(
		private val validateInit: Boolean,
	) : S2SpringAdapterBase<TestEntity, TestState, TestEvent, String>() {
		override fun automate(): S2Automate = s2 {
			name = "AdapterTest"
			init<CreateCmd> {
				to = TestState.Created
				role = TestRole
			}
		}

		override fun validateInitTransitions(): Boolean = validateInit

		fun exposedGuards(): List<Guard<TestState, String, TestEntity, TestEvent, S2Automate>> = guards()
	}

	@Test
	fun `init transitions are not validated by default`() {
		val guards = TestAdapter(validateInit = false).exposedGuards()
		assertThat(guards).hasSize(1)
		assertThat(guards.single()).isInstanceOf(TransitionStateGuard::class.java)
	}

	@Test
	fun `init transitions are validated when the check is opted in`() {
		val guards = TestAdapter(validateInit = true).exposedGuards()
		assertThat(guards).hasSize(2)
		assertThat(guards[0]).isInstanceOf(TransitionStateGuard::class.java)
		assertThat(guards[1]).isInstanceOf(InitTransitionStateGuard::class.java)
	}
}
