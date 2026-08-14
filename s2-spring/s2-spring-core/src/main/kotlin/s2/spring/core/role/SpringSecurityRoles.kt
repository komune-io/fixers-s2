package s2.spring.core.role

import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContextHolder
import s2.dsl.automate.S2RoleValue

/** Prefix Spring Security conventionally puts in front of role authorities. */
const val ROLE_AUTHORITY_PREFIX: String = "ROLE_"

/**
 * Roles of the current Spring Security context, as [S2RoleValue].
 *
 * Looks first at the reactive `SecurityContext` carried by the coroutine's Reactor context —
 * how the framework's own HTTP entry points and `s2-test-bdd`'s `authedContext()` inject the
 * authenticated JWT — then falls back to the thread-bound [SecurityContextHolder] for servlet
 * stacks. Returns an empty set when nothing is authenticated.
 *
 * Authorities are used rather than raw JWT claims because that is where Spring Security has
 * already normalised whatever the identity provider issued; the [ROLE_AUTHORITY_PREFIX] it
 * adds is stripped back off.
 *
 * Spring Security is only needed on the classpath when this is actually called, i.e. when an
 * adapter opts into `validateRoles()`.
 */
suspend fun springSecurityRoles(): Set<S2RoleValue> {
	val authentication = reactiveAuthentication() ?: threadBoundAuthentication()
	return authentication.toS2Roles()
}

private suspend fun reactiveAuthentication(): Authentication? =
	ReactiveSecurityContextHolder.getContext().awaitFirstOrNull()?.authentication

private fun threadBoundAuthentication(): Authentication? =
	SecurityContextHolder.getContext()?.authentication

private fun Authentication?.toS2Roles(): Set<S2RoleValue> {
	if (this == null || !isAuthenticated) return emptySet()
	return authorities.orEmpty()
		.mapNotNull { it.authority }
		.map { S2RoleValue(it.removePrefix(ROLE_AUTHORITY_PREFIX)) }
		.toSet()
}
