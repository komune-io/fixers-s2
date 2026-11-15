package s2.spring.sourcing.snap.redis

import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode

/**
 * The texts a view writes into each stored document folded ([foldText]), one per text attribute: the index reads them
 * in place of the model's own texts, so that a search, folded alike, finds a text whatever the accents and case of
 * either, on every Redis Stack. An array's texts are joined into one: Redis Stack 6.2 leaves out of the index a
 * document whose text path holds two values or more.
 */
internal class FoldedTexts(private val folded: List<Folded>) : DerivedField {

    data class Folded(val attribute: String, val segments: List<Segment>)

    /** One name of a path, its value walked member by member when [isArray]. */
    data class Segment(val name: String, val isArray: Boolean)

    override val documentField: String get() = DOCUMENT_FIELD

    override val isEmpty: Boolean get() = folded.isEmpty()

    /** The folded texts of [document]; an attribute holding no string is left out, as a missing text. */
    override fun valueOf(document: ObjectNode, id: String): ObjectNode = document.objectNode().apply {
        folded.forEach { field ->
            val texts = field.segments.fold(listOf<JsonNode>(document)) { nodes, segment ->
                nodes.flatMap { node -> segment.walk(node.get(segment.name)) }
            }.filter { it.isString }
            if (texts.isNotEmpty()) put(field.attribute, foldText(texts.joinToString(" ") { it.stringValue() }))
        }
    }

    private fun Segment.walk(value: JsonNode?): List<JsonNode> = when {
        value == null -> emptyList()
        !isArray -> listOf(value)
        value.isArray -> value.toList()
        else -> emptyList()
    }

    companion object {
        const val DOCUMENT_FIELD = "__s2_text"

        private val SEGMENT = Regex("([A-Za-z0-9_]+)(\\[\\*])?")
        private val ATTRIBUTE = Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)*")

        /**
         * Null when [path] is not dotted names, each walked whole with `[*]` or not, or [attribute] not dotted names:
         * the folded text's path is built from the attribute.
         */
        fun folded(attribute: String, path: String): Folded? {
            val segments = path.split('.').map { name ->
                SEGMENT.matchEntire(name)?.let { Segment(name = it.groupValues[1], isArray = it.groupValues[2] != "") }
            }
            return if (ATTRIBUTE.matches(attribute) && segments.all { it != null }) {
                Folded(attribute = attribute, segments = segments.filterNotNull())
            } else {
                null
            }
        }

        fun path(folded: Folded): String = "$.$DOCUMENT_FIELD[\"${folded.attribute}\"]"
    }
}
