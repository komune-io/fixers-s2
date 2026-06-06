package s2.automate.core.persist

import s2.dsl.automate.ErrorCategory
import s2.dsl.automate.S2Error

sealed interface PersistOutcome<out EVENT> {
    val msgId: String

    sealed interface Failure<out EVENT> : PersistOutcome<EVENT> {
        val error: S2Error
        val category: ErrorCategory
    }

    data class Success<out EVENT>(
        override val msgId: String,
        val event: EVENT,
        val metadata: Map<String, String> = emptyMap(),
    ) : PersistOutcome<EVENT>

    data class Rejected<out EVENT>(
        override val msgId: String,
        override val error: S2Error,
    ) : Failure<EVENT> {
        override val category: ErrorCategory = ErrorCategory.Rejected
    }

    data class Transient<out EVENT>(
        override val msgId: String,
        override val error: S2Error,
    ) : Failure<EVENT> {
        override val category: ErrorCategory = ErrorCategory.Transient
    }

    data class Indeterminate<out EVENT>(
        override val msgId: String,
        override val error: S2Error,
    ) : Failure<EVENT> {
        override val category: ErrorCategory = ErrorCategory.Indeterminate
    }

    data class Conflict<out EVENT>(
        override val msgId: String,
        override val error: S2Error,
    ) : Failure<EVENT> {
        override val category: ErrorCategory = ErrorCategory.Conflict
    }

    fun eventOrNull(): EVENT? = (this as? Success<EVENT>)?.event
}
