package s2.bdd.data

import f2.dsl.cqrs.Event
import s2.bdd.auth.AuthedUser

typealias TestContextKey = String

open class TestContext {
    val entityLists = mutableMapOf<String, TestEntities<*, *>>()

    var authedUser: AuthedUser? = null

    val errors = ExceptionList()
    val events = mutableListOf<Event>()

    fun <K: Any, V> testEntities(name: String): TestEntities<K, V> = TestEntities<K, V>(name)
        .also { entityLists[name] = it }

    open fun reset() {
        resetEnv()
        entityLists.values.forEach(TestEntities<*, *>::reset)
        errors.reset()
        events.clear()
        authedUser = null
    }

    open fun resetEnv() {}
}
