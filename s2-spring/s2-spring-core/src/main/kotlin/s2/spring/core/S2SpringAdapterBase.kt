package s2.spring.core

import org.springframework.beans.factory.annotation.Autowired
import s2.automate.core.appevent.publisher.AutomateEventPublisher
import s2.automate.core.config.S2BatchProperties
import s2.automate.core.context.AutomateContext
import s2.automate.core.guard.Guard
import s2.automate.core.guard.GuardVerifier
import s2.automate.core.guard.GuardVerifierImpl
import s2.automate.core.guard.InitTransitionStateGuard
import s2.automate.core.guard.RoleGuard
import s2.automate.core.guard.TransitionStateGuard
import s2.dsl.automate.Evt
import s2.dsl.automate.S2Automate
import s2.dsl.automate.S2RoleValue
import s2.dsl.automate.S2State
import s2.dsl.automate.model.WithS2Id
import s2.dsl.automate.model.WithS2State
import s2.spring.core.publisher.SpringEventPublisher
import s2.spring.core.role.springSecurityRoles

abstract class S2SpringAdapterBase<ENTITY, STATE, EVENT, ID> where
STATE : S2State,
ENTITY : WithS2State<STATE>,
ENTITY : WithS2Id<ID>,
EVENT: Evt{

	protected open fun automateContext() = AutomateContext(automate(), batchParams)

	protected open fun guardExecutor(
		automateAppEventPublisher: AutomateEventPublisher<STATE, ID, ENTITY, S2Automate>,
	): GuardVerifier<STATE, ID, ENTITY, EVENT, S2Automate> {
		return GuardVerifierImpl(
			guards = guards(),
			publisher = automateAppEventPublisher
		)
	}

	protected open fun automateAppEventPublisher(eventPublisher: SpringEventPublisher)
			: AutomateEventPublisher<STATE, ID, ENTITY, S2Automate> {
		return AutomateEventPublisher(eventPublisher)
	}

	/**
	 * Whether init commands are checked against the init transitions declared by the automate.
	 *
	 * Disabled by default: until now init transitions were never validated, and an automate
	 * may declare its creation transition in a way that exposes no init transition at all
	 * (`transaction<CreateCmd> { to = ... }` without `from`). Enabling the check on such an
	 * automate rejects every creation, so it is opt-in.
	 *
	 * Override and return `true` to have [InitTransitionStateGuard] reject any init command
	 * that is not declared with `init<...>` in the automate.
	 */
	protected open fun validateInitTransitions(): Boolean = false

/**
	 * Whether the `role` declared on each transition is enforced.
	 *
	 * Disabled by default, and it must stay that way: until now roles were declared and
	 * serialized but never checked, so consumers have automates whose S2 role class names
	 * do not necessarily line up with the role names their identity provider issues.
	 * Turning enforcement on for them would reject valid traffic everywhere.
	 *
	 * Override and return `true` to have [RoleGuard] reject any command whose transition
	 * declares roles that [currentRoles] does not return, with `ERROR_MISSING_ROLE`.
	 * Verify [currentRoles] against your own role names first.
	 */
	protected open fun validateRoles(): Boolean = false

	/**
	 * Roles held by the caller of the command currently being processed, used by [RoleGuard]
	 * when [validateRoles] is on. Resolved per command, not once at startup.
	 *
	 * Defaults to the roles of the current Spring Security context — the reactive
	 * `SecurityContext` when the call runs inside one, else the thread-bound one — with the
	 * `ROLE_` authority prefix stripped. Override when your roles live somewhere else, or to
	 * map identity-provider role names onto your [s2.dsl.automate.S2Role] class names.
	 *
	 * Only called when [validateRoles] returns `true`, so applications that never opt in do
	 * not need Spring Security on the classpath.
	 */
	protected open suspend fun currentRoles(): Set<S2RoleValue> = springSecurityRoles()

	protected open fun guards(): List<Guard<STATE, ID, ENTITY, EVENT, S2Automate>> = listOfNotNull(
		TransitionStateGuard(),
		InitTransitionStateGuard<STATE, ID, ENTITY, EVENT, S2Automate>()
			.takeIf { validateInitTransitions() },
		RoleGuard<STATE, ID, ENTITY, EVENT> { currentRoles() }
			.takeIf { validateRoles() },
	)

	@Autowired
	lateinit var batchParams: S2BatchProperties

	abstract fun automate(): S2Automate

}
