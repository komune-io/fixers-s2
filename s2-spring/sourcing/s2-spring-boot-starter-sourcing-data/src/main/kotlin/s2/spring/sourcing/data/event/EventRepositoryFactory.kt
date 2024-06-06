package s2.spring.sourcing.data.event

import kotlin.reflect.KClass
import kotlinx.serialization.json.Json
import s2.dsl.automate.Evt
import s2.dsl.automate.model.WithS2Id
import s2.sourcing.dsl.event.EventRepository

interface EventRepositoryFactory {


    fun <EVENT, ID> create(
        eventType: KClass<EVENT>,
        json: Json
    ): EventRepository<EVENT, ID> where EVENT : Evt, EVENT : WithS2Id<ID>
}
