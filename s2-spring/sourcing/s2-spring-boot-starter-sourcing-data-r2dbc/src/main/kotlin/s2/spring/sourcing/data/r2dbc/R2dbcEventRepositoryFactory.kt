package s2.spring.sourcing.data.r2dbc

import kotlin.reflect.KClass
import kotlinx.serialization.json.Json
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.r2dbc.core.DatabaseClient
import s2.dsl.automate.Evt
import s2.dsl.automate.model.WithS2Id
import s2.sourcing.dsl.event.EventRepository
import s2.spring.sourcing.data.event.EventRepositoryFactory

class R2dbcEventRepositoryFactory(
    private val databaseClient: DatabaseClient,
    private val r2dbcEntityTemplate: R2dbcEntityTemplate,
): EventRepositoryFactory {

    override fun <EVENT, ID : Any> create(
        eventType: KClass<EVENT>,
        json: Json
    ): EventRepository<EVENT, ID> where EVENT : WithS2Id<ID>, EVENT : Evt =
        R2dbcEventRepository(
            json = json,
            databaseClient = databaseClient,
            r2dbcEntityTemplate = r2dbcEntityTemplate,
            eventType = eventType,
        )
}
