package s2.bdd.assertion

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.delay

/**
 * Polls [condition] until it holds, for effects that land after the command returns — an
 * `@EventListener` the publisher does not wait for, a scheduled pass — instead of sleeping a fixed time.
 * The condition is checked at least once, and once more after [timeout] before failing.
 */
suspend fun awaitUntil(
    timeout: Duration = 10.seconds,
    interval: Duration = 100.milliseconds,
    description: String = "condition",
    condition: suspend () -> Boolean,
) {
    val deadline = TimeSource.Monotonic.markNow() + timeout
    while (!condition()) {
        if (deadline.hasPassedNow()) {
            throw AssertionError("Timed out after $timeout waiting for $description")
        }
        delay(interval)
    }
}
