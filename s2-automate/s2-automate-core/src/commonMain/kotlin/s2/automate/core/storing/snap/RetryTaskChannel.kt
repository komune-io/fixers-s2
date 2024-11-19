package s2.automate.core.storing.snap

import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class RetryTask<ID, ENTITY, EVENT>(
    val id: ID,
    val event: EVENT,
    val result: CompletableDeferred<Pair<Pair<ENTITY, EVENT>?, Throwable?>>,
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

    private val persistChannel = Channel<RetryTask<Any, Any, Any>>()

    init {
        launchPersistWorker()
    }

    private fun CoroutineScope.launchPersistWorker() = launch {
        for (task in persistChannel) {
            val result = retry {
                task.persist(task.event)
            }
            task.result.complete(result)
        }
    }

    suspend fun <ID, ENTITY, EVENT> addToPersistQueue(
        id: ID,
        event: EVENT,
        persist: suspend (EVENT) -> Pair<ENTITY, EVENT>
    ): Pair<ENTITY, EVENT> {
        val resultDeferred = CompletableDeferred<Pair<Pair<ENTITY, EVENT>?, Throwable?>>()
        val task = RetryTask(id, event, resultDeferred, persist) as RetryTask<Any, Any, Any>
        persistChannel.send(task)
        val (entity, exception) = resultDeferred.await()
        if(exception != null) throw exception
        return entity!!
    }


    @Suppress("NestedBlockDepth")
    private suspend fun <T> retry(
        block: suspend () -> T
    ): Pair<T?, Throwable?> {
        var attempts = 0
        while (true) {
            try {
                return block() to null
            } catch (e: Throwable) {
                if(retryOn.isInstance(e)) {
                    attempts++
                    if (attempts >= maxAttempts) {
                        return (null to e)
                    }
                    delay(delayMillis)
                } else {
                    return (null to e)
                }
            }
        }
    }


    // Call this method to cancel all the child coroutines when the class is no longer needed
    fun cancelAllCoroutines() {
        supervisorJob.cancel()
    }
}
