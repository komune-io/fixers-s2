package s2.bdd

import f2.dsl.cqrs.exception.F2Exception
import io.cucumber.core.backend.CucumberInvocationTargetException
import io.cucumber.datatable.CucumberDataTableException
import io.komune.f2.spring.boot.auth.config.WebSecurityConfig
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.newCoroutineContext
import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.runBlocking
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import reactor.core.publisher.Mono
import reactor.util.context.Context
import s2.automate.core.error.AutomateException
import s2.bdd.data.TestContext
import java.util.UUID

abstract class CucumberStepsDefinition {
    protected abstract val context: TestContext

    protected fun String?.orRandom() = this ?: UUID.randomUUID().toString()

    protected fun String.parseNullValue() = takeUnless { it == "null" }

    protected fun String?.parseNullableOrDefault(
        default: String?, parse: (String) -> String? = { this }
    ) = parseNullableOrDefault<String>(default, parse)

    protected fun <T> String?.parseNullableOrDefault(default: T?, parse: (String) -> T?): T? = when (this) {
        null -> default
        "null" -> null
        else -> parse(this)
    }

    protected open fun step(block: suspend () -> Unit) {
        step({ e ->
            e !is AutomateException
                    && e !is F2Exception
        }, block)
    }

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    protected open fun step(propagateException: (Exception) -> Boolean = { true }, block: suspend () -> Unit) {
        runBlocking(GlobalScope.newCoroutineContext(authedContext())) {
            try {
                block()
            } catch (e: CucumberDataTableException) {
                val actualException = (e.cause as? CucumberInvocationTargetException)
                    ?.invocationTargetExceptionCause
                    ?: e
                actualException.printStackTrace()
                throw actualException
            } catch (e: CucumberInvocationTargetException) {
                val actualException = e.invocationTargetExceptionCause ?: e
                actualException.printStackTrace()
                throw actualException
            } catch (e: Exception) {
                e.printStackTrace()
                context.errors.add(e)
                if (propagateException(e)) {
                    throw e
                }
            }
        }
    }

    protected open fun authedContext(): ReactorContext {
        val authedUser = context.authedUser
            ?: return ReactorContext(Context.of(SecurityContext::class.java, Mono.empty<SecurityContext>()))

        val securityContext = mapOf(
            "realm_access" to mapOf(
                "roles" to authedUser.roles
            ),
            "memberOf" to authedUser.memberOf,
            "sub" to authedUser.id
        ).let { claims -> Jwt("fake", null, null, mapOf("header" to "fake"), claims) }
            .let { jwt ->
                JwtAuthenticationToken(jwt, authedUser.roles.map {
                    SimpleGrantedAuthority("${WebSecurityConfig.ROLE_PREFIX}$it")
                })
            }
            .let(::SecurityContextImpl)
        return ReactorContext(Context.of(SecurityContext::class.java, Mono.just(securityContext)))
    }
}
