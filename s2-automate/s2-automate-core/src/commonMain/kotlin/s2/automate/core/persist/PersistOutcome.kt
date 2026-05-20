package s2.automate.core.persist

import s2.dsl.automate.S2Error

sealed interface PersistOutcome<EVENT> {
    val commandId: String

    sealed interface Failure<EVENT> : PersistOutcome<EVENT> {
        val error: S2Error
        val category: ErrorCategory
    }

    data class Success<EVENT>(
        override val commandId: String,
        val event: EVENT,
        val transactionId: String,
        val blockNumber: Long,
        val payload: ByteArray? = null,
    ) : PersistOutcome<EVENT> {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Success<*>) return false
            return commandId == other.commandId &&
                event == other.event &&
                transactionId == other.transactionId &&
                blockNumber == other.blockNumber &&
                payload.contentEquals(other.payload)
        }

        override fun hashCode(): Int {
            var result = commandId.hashCode()
            result = 31 * result + (event?.hashCode() ?: 0)
            result = 31 * result + transactionId.hashCode()
            result = 31 * result + blockNumber.hashCode()
            result = 31 * result + (payload?.contentHashCode() ?: 0)
            return result
        }
    }

    data class Rejected<EVENT>(
        override val commandId: String,
        override val error: S2Error,
    ) : Failure<EVENT> {
        override val category: ErrorCategory = ErrorCategory.Rejected
    }

    data class Transient<EVENT>(
        override val commandId: String,
        override val error: S2Error,
    ) : Failure<EVENT> {
        override val category: ErrorCategory = ErrorCategory.Transient
    }

    data class Indeterminate<EVENT>(
        override val commandId: String,
        override val error: S2Error,
    ) : Failure<EVENT> {
        override val category: ErrorCategory = ErrorCategory.Indeterminate
    }

    data class Conflict<EVENT>(
        override val commandId: String,
        override val error: S2Error,
    ) : Failure<EVENT> {
        override val category: ErrorCategory = ErrorCategory.Conflict
    }

    fun eventOrNull(): EVENT? = (this as? Success<EVENT>)?.event
}

private fun ByteArray?.contentEquals(other: ByteArray?): Boolean = when {
    this === other -> true
    this == null || other == null -> false
    else -> this.contentEquals(other)
}
