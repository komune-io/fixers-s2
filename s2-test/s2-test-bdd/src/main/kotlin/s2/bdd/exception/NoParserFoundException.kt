package s2.bdd.exception

import kotlin.reflect.KClass

class NoParserFoundException(
    type: KClass<*>
): UnsupportedOperationException("No entry parser registered for type ${type.simpleName}")
