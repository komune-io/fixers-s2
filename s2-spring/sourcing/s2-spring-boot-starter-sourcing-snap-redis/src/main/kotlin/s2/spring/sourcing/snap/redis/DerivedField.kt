package s2.spring.sourcing.snap.redis

import tools.jackson.databind.node.ObjectNode

/** A field a view computes from each document it stores and writes into it, under [documentField]. */
internal interface DerivedField {
    val documentField: String

    /** Whether the view derives nothing here, and writes no [documentField]. */
    val isEmpty: Boolean

    fun valueOf(document: ObjectNode, id: String): ObjectNode
}

/**
 * Replaces the [DerivedField.documentField] [document] holds: a model reading stored documents back carries old
 * values.
 */
internal fun DerivedField.write(document: ObjectNode, id: String) {
    if (isEmpty) return
    val _ = document.set(documentField, valueOf(document = document, id = id))
}
