package s2.automate.core.storing.snap

import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class RetryTask<ENTITY, EVENT>(
    val event: EVENT,
    val result: CompletableDeferred<Result<Pair<ENTITY, EVENT>>>,
    val persist: suspend (EVENT) -> Pair<ENTITY, EVENT>,
)

class RetryTaskChannel(
    private val maxAttempts: Int = 5,
    private val delayMillis: Long = 1000,
    private val retryOn: KClass<*>
) : CoroutineScope {

    private val supervisorJob = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + supervisorJob

    private val persistChannel = Channel<suspend () -> Unit>()

    init {
        launchPersistWorker()
    }

    private fun CoroutineScope.launchPersistWorker() = launch {
        for (task in persistChannel) {
            try {
                task()
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                // a failing task must not kill the single worker, or every
                // subsequent addToPersistQueue send would suspend forever
            }
        }
    }

    suspend fun <ENTITY, EVENT> addToPersistQueue(
        event: EVENT,
        persist: suspend (EVENT) -> Pair<ENTITY, EVENT>
    ): Pair<ENTITY, EVENT> {
        val resultDeferred = CompletableDeferred<Result<Pair<ENTITY, EVENT>>>()
        val task = RetryTask(event, resultDeferred, persist)
        persistChannel.send {
            task.result.complete(retry { task.persist(task.event) })
        }
        return resultDeferred.await().getOrThrow()
    }

    private suspend fun <T> retry(
        block: suspend () -> T
    ): Result<T> {
        var attempts = 0
        while (true) {
            val result = runCatching { block() }
            val failure = result.exceptionOrNull() ?: return result
            if (!retryOn.isInstance(failure)) return result
            attempts++
            if (attempts >= maxAttempts) return result
            delay(delayMillis)
        }
    }


    // Call this method to cancel all the child coroutines when the class is no longer needed
    fun cancelAllCoroutines() {
        supervisorJob.cancel()
    }
}
