package s2.automate.core.persist

sealed interface PersistOutcome<EVENT> {
    val commandId: String

    data class Committed<EVENT>(
        override val commandId: String,
        val event: EVENT,
        val transactionId: String,
        val blockNumber: Long,
    ) : PersistOutcome<EVENT>

    data class Rejected<EVENT>(
        override val commandId: String,
        val errorCode: String,
        val errorMessage: String,
    ) : PersistOutcome<EVENT>

    data class Transient<EVENT>(
        override val commandId: String,
        val errorCode: String,
        val errorMessage: String,
    ) : PersistOutcome<EVENT>

    data class Indeterminate<EVENT>(
        override val commandId: String,
        val errorCode: String,
        val errorMessage: String,
    ) : PersistOutcome<EVENT>

    data class Conflict<EVENT>(
        override val commandId: String,
        val errorCode: String,
        val errorMessage: String,
    ) : PersistOutcome<EVENT>

    fun eventOrNull(): EVENT? = (this as? Committed<EVENT>)?.event
}
