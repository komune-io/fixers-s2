package s2.bdd

import f2.dsl.cqrs.exception.F2Exception
import io.cucumber.core.backend.CucumberInvocationTargetException
import io.cucumber.datatable.CucumberDataTableException
import io.komune.f2.spring.boot.auth.ROLE_PREFIX
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.newCoroutineContext
import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import reactor.core.publisher.Mono
import reactor.util.context.Context
import s2.automate.core.error.AutomateException
import s2.bdd.data.TestContext

private const val STEP_FAILED = "Step execution failed"

abstract class CucumberStepsDefinition {
    private val logger = LoggerFactory.getLogger(CucumberStepsDefinition::class.java)!!

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

    // Each Cucumber wrapper is unwrapped to the step's own failure; any other failure is recorded for the
    // assertion steps that follow, then rethrown unless the step expects it.
    @Suppress("ThrowsCount", "TooGenericExceptionCaught", "SwallowedException")
    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    protected open fun step(propagateException: (Exception) -> Boolean = { true }, block: suspend () -> Unit) {
        runBlocking(GlobalScope.newCoroutineContext(authedContext())) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: CucumberDataTableException) {
                val actualException = (e.cause as? CucumberInvocationTargetException)
                    ?.cause
                    ?: e
                logger.error(STEP_FAILED, actualException)
                throw actualException
            } catch (e: CucumberInvocationTargetException) {
                val cause = e.cause
                logger.error(STEP_FAILED, cause)
                throw cause
            } catch (e: Exception) {
                logger.error(STEP_FAILED, e)
                val cause = e.cause
                if (cause is F2Exception) {
                    context.errors.add(cause)
                } else {
                    context.errors.add(e)
                }
                if (propagateException(e)) {
                    throw e
                }
            }
        }
    }

    protected open fun authedContext(): ReactorContext {
        val authedUser = context.authedUser
            ?: return ReactorContext(Context.of(SecurityContext::class.java, Mono.empty<SecurityContext>()))

        val claims = buildMap<String, Any> {
            put(key = "realm_access", value = mapOf("roles" to authedUser.roles))
            authedUser.memberOf?.let { put(key = "memberOf", value = it) }
            put(key = "sub", value = authedUser.id)
        }
        val jwt = Jwt("fake", null, null, mapOf("header" to "fake"), claims)
        val authorities = authedUser.roles.map { SimpleGrantedAuthority("${ROLE_PREFIX}$it") }
        val securityContext = SecurityContextImpl(JwtAuthenticationToken(jwt, authorities))
        return ReactorContext(Context.of(SecurityContext::class.java, Mono.just(securityContext)))
    }
}
