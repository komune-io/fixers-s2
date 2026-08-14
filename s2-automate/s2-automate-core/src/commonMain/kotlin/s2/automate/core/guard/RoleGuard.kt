package s2.automate.core.guard

import s2.automate.core.context.InitTransitionContext
import s2.automate.core.context.TransitionContext
import s2.automate.core.error.missingRoleError
import s2.dsl.automate.Cmd
import s2.dsl.automate.Msg
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2RoleValue
import s2.dsl.automate.S2State
import s2.dsl.automate.S2Transition
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.dsl.automate.toValue

/**
 * Enforces the `role` every automate transition declares.
 *
 * Until this guard exists, `role = ...` in an `s2 { }` block was written by the builders,
 * serialized into [S2Transition], and read by nothing: any caller could run any transition.
 *
 * The guard resolves the transition the command is about to take, collects the roles declared
 * for it, and rejects the command with `ERROR_MISSING_ROLE` unless [rolesProvider] returns at
 * least one of them. Role names are compared **case-insensitively** by their
 * [S2RoleValue.name] (the simple class name of the [s2.dsl.automate.S2Role]), so an identity
 * provider emitting `admin` satisfies a transition declaring `Admin`.
 *
 * **This guard is opt-in and MUST stay opt-in.** Existing consumers declare roles that were
 * never enforced, and their runtime role names very likely do not line up with their S2 role
 * class names; switching it on by default would reject valid traffic across the board. See
 * `S2SpringAdapterBase.validateRoles()`.
 *
 * A command matching no declared transition is left alone: deciding whether that command is
 * legal at all is [TransitionStateGuard]'s job, and this guard has no role to check against.
 *
 * @param rolesProvider the roles held by the current caller, resolved per command — on Spring
 * this reads the current `SecurityContext`.
 */
class RoleGuard<STATE, ID, ENTITY, EVENT>(
	private val rolesProvider: suspend () -> Set<S2RoleValue>,
) : GuardAdapter<STATE, ID, ENTITY, EVENT, S2Automate>() where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID> {

	override suspend fun evaluateInit(context: InitTransitionContext<S2Automate>): GuardResult {
		val required = context.automateContext.automate.transitions
			.filter { it.from == null && it.matches(context.msg) }
			.map { it.role }
		return check(required)
	}

	override suspend fun <COMMAND : Cmd> evaluateTransition(
		context: TransitionContext<STATE, ID, ENTITY, S2Automate, COMMAND>
	): GuardResult {
		val command = context.command.data
		val required = context.automateContext.automate
			.getAvailableTransitions(context.entity.s2State())
			.filter { it.matches(command) }
			.map { it.role }
		return check(required)
	}

	private suspend fun check(required: List<S2RoleValue>): GuardResult {
		// No declared transition matches this command: nothing to enforce here.
		if (required.isEmpty()) return GuardResult.valid()

		val actual = rolesProvider()
		val actualNames = actual.map { it.name.lowercase() }.toSet()
		return if (required.any { it.name.lowercase() in actualNames }) {
			GuardResult.valid()
		} else {
			GuardResult.error(
				missingRoleError(
					required = required.map { it.name },
					actual = actual.map { it.name },
				)
			)
		}
	}

	private fun S2Transition.matches(msg: Msg): Boolean = action.name == msg::class.toValue().name
}
