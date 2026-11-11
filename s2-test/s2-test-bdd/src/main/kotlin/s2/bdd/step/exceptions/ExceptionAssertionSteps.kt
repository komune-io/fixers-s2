package s2.bdd.step.exceptions

import f2.dsl.cqrs.error.HttpRefusal
import f2.spring.exception.ConflictException
import f2.spring.exception.ForbiddenAccessException
import f2.spring.exception.NotFoundException
import f2.spring.exception.UnreadableBodyException
import io.cucumber.java8.En
import s2.bdd.S2CucumberStepsDefinition
import s2.bdd.assertion.AssertionBdd
import s2.bdd.assertion.exceptions
import s2.bdd.data.parser.extract
import s2.bdd.data.parser.safeExtract

class ExceptionAssertionSteps: En, S2CucumberStepsDefinition()  {
    init {
        DataTableType(::exceptionAssertionParams)

        Then("An exception should be thrown:") { params: ExceptionAssertionParams ->
            step {
                AssertionBdd.exceptions(context)
                    .assertThat(params.code.toExceptionClass())
                    .hasBeenThrown(params.times)
            }
        }

        Then("No exception should be thrown:") { params: ExceptionAssertionParams ->
            step {
                AssertionBdd.exceptions(context)
                    .assertThat(params.code.toExceptionClass())
                    .hasNotBeenThrown()
            }
        }
    }

    private fun Int.toExceptionClass() = when (this) {
        HttpRefusal.INVALID_PAYLOAD.status -> UnreadableBodyException::class
        HttpRefusal.FORBIDDEN.status -> ForbiddenAccessException::class
        HttpRefusal.NOT_FOUND.status -> NotFoundException::class
        HttpRefusal.CONFLICT.status -> ConflictException::class
        else -> throw IllegalArgumentException("Unknown exception code [$this]")
    }

    private fun exceptionAssertionParams(entry: Map<String, String>) = ExceptionAssertionParams(
        code = entry.safeExtract<Int>("code"),
        times = entry.extract<Int>("times") ?: 1
    )

    private data class ExceptionAssertionParams(
        val code: Int,
        val times: Int
    )
}
