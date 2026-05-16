package s2.automate.core.persist

sealed interface PersistOutcome<EVENT> {
    val commandId: String

    sealed interface Failure<EVENT> : PersistOutcome<EVENT> {
        val errorCode: String
        val errorMessage: String
    }

    data class Success<EVENT>(
        override val commandId: String,
        val event: EVENT,
        val transactionId: String,
        val blockNumber: Long,
    ) : PersistOutcome<EVENT>

    data class Rejected<EVENT>(
        override val commandId: String,
        override val errorCode: String,
        override val errorMessage: String,
    ) : Failure<EVENT>

    data class Transient<EVENT>(
        override val commandId: String,
        override val errorCode: String,
        override val errorMessage: String,
    ) : Failure<EVENT>

    data class Indeterminate<EVENT>(
        override val commandId: String,
        override val errorCode: String,
        override val errorMessage: String,
    ) : Failure<EVENT>

    data class Conflict<EVENT>(
        override val commandId: String,
        override val errorCode: String,
        override val errorMessage: String,
    ) : Failure<EVENT>

    fun eventOrNull(): EVENT? = (this as? Success<EVENT>)?.event
}
