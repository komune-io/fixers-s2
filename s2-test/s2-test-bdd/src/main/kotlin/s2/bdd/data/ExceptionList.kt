package s2.bdd.data

import kotlin.reflect.KClass

class ExceptionList {
    private val exceptions = mutableListOf<Exception>()

    val list: List<Exception>
        get() = exceptions.toList()

    fun add(e: Exception) {
        exceptions.add(e)
    }

    fun <E: Exception> lastOfType(klass: KClass<E>): E? = exceptions.filterIsInstance(klass.java).lastOrNull()

    fun <E: Exception> filterIsInstance(klass: KClass<E>): List<E> = list.filterIsInstance(klass.java)

    fun reset() {
        exceptions.clear()
    }
}
