package s2.automate.core.storing.snap

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KClass
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
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
    private val retryOn: KClass<*>,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : CoroutineScope {

    private val supervisorJob = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = dispatcher + supervisorJob

    private val persistChannel = Channel<suspend () -> Unit>()

    init {
        launchPersistWorker()
    }

    private fun CoroutineScope.launchPersistWorker() {
        // retry() turns every failure but cancellation into a Result, and each task runs in its own
        // child job, so a cancelled task does not end this single worker: a dead worker would
        // suspend every later send forever.
        val _ = launch {
            for (task in persistChannel) {
                launch { task() }.join()
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
            try {
                check(task.result.complete(retry { task.persist(task.event) })) { "A persist task completed twice" }
            } catch (e: CancellationException) {
                check(task.result.completeExceptionally(e)) { "A persist task completed twice" }
                throw e
            }
        }
        return resultDeferred.await().getOrThrow()
    }

    private suspend fun <T> retry(
        block: suspend () -> T
    ): Result<T> {
        var attempts = 1
        var result = attempt(block)
        while (attempts < maxAttempts && result.exceptionOrNull()?.let { retryOn.isInstance(it) } == true) {
            delay(delayMillis)
            result = attempt(block)
            attempts++
        }
        return result
    }

    // retryOn decides which failures are retried: any other failure must reach the caller as a Result.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> attempt(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }


    // Call this method to cancel all the child coroutines when the class is no longer needed
    fun cancelAllCoroutines() {
        supervisorJob.cancel()
    }
}
