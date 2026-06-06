package s2.automate.core.persist

import s2.dsl.automate.ErrorCategory
import s2.dsl.automate.S2Error

sealed interface PersistOutcome<EVENT> {
    val msgId: String

    sealed interface Failure<EVENT> : PersistOutcome<EVENT> {
        val error: S2Error
        val category: ErrorCategory
    }

    data class Success<EVENT>(
        override val msgId: String,
        val event: EVENT,
        val metadata: Map<String, String> = emptyMap(),
    ) : PersistOutcome<EVENT>

    data class Rejected<EVENT>(
        override val msgId: String,
        override val error: S2Error,
    ) : Failure<EVENT> {
        override val category: ErrorCategory = ErrorCategory.Rejected
    }

    data class Transient<EVENT>(
        override val msgId: String,
        override val error: S2Error,
    ) : Failure<EVENT> {
        override val category: ErrorCategory = ErrorCategory.Transient
    }

    data class Indeterminate<EVENT>(
        override val msgId: String,
        override val error: S2Error,
    ) : Failure<EVENT> {
        override val category: ErrorCategory = ErrorCategory.Indeterminate
    }

    data class Conflict<EVENT>(
        override val msgId: String,
        override val error: S2Error,
    ) : Failure<EVENT> {
        override val category: ErrorCategory = ErrorCategory.Conflict
    }

    fun eventOrNull(): EVENT? = (this as? Success<EVENT>)?.event
}
