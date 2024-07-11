package s2.bdd.data

import s2.bdd.exception.EntityNotInitializedException

class TestEntities<KEY: Any, ITEM>(
    private val name: String
): Iterable<Map.Entry<KEY, ITEM?>> {
    private val mutableEntities = mutableMapOf<KEY, ITEM?>()

    val items: List<ITEM>
        get() = mutableEntities.values.mapNotNull { it }

    val keys: Set<KEY>
        get() = mutableEntities.keys

    private var lastUsedPair: Pair<KEY, ITEM?>? = null

    val lastUsedKey: KEY
        get() = lastUsedPair?.first ?: throw EntityNotInitializedException("lastUsed", name)

    val lastUsedOrNull: ITEM?
        get() = lastUsedPair?.second

    val lastUsed: ITEM
        get() = lastUsedOrNull ?: throw EntityNotInitializedException("lastUsed", name)

    val size
        get() = items.size

    operator fun get(key: KEY): ITEM? = mutableEntities[key]
        .also { lastUsedPair = key to it }

    fun safeGet(key: KEY): ITEM = get(key) ?: throw EntityNotInitializedException(key.toString(), name)

    fun containsKey(key: KEY): Boolean = mutableEntities.containsKey(key)

    operator fun set(key: KEY, entity: ITEM?) {
        mutableEntities[key] = entity
        lastUsedPair = key to entity
    }

    fun reset() {
        lastUsedPair = null
        mutableEntities.clear()
    }

    inline fun register(key: KEY, block: () -> ITEM?) {
        try {
            val item = block()
            set(key, item)
        } catch (e: Exception) {
            set(key, null)
            throw e
        }
    }

    fun putAll(vararg entries: Pair<KEY, ITEM>) {
        entries.forEach { (id, entity) -> set(id, entity) }
    }

    fun putAll(from: Map<KEY, ITEM>) {
        from.forEach { (id, entity) -> set(id, entity) }
    }

    override fun iterator(): Iterator<Map.Entry<KEY, ITEM?>> {
        return mutableEntities.iterator()
    }
}
