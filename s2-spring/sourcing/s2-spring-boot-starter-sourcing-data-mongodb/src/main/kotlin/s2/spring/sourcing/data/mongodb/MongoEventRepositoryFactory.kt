package s2.spring.sourcing.data.mongodb

import kotlin.reflect.KClass
import kotlinx.serialization.json.Json
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.repository.support.ReactiveMongoRepositoryFactory
import s2.dsl.automate.Evt
import s2.dsl.automate.model.WithS2Id
import s2.sourcing.dsl.event.EventRepository
import s2.spring.sourcing.data.event.EventRepositoryFactory

class MongoEventRepositoryFactory(
    private val mongoOperations: ReactiveMongoOperations
): EventRepositoryFactory {

    override fun <EVENT, ID> create(
        eventType: KClass<EVENT>,
        json: Json
    ): EventRepository<EVENT, ID> where EVENT : Evt, EVENT : WithS2Id<ID> {
        val repositoryFactorySupport = ReactiveMongoRepositoryFactory(mongoOperations)
        val repository = repositoryFactorySupport.getRepository(SpringDataEventRepository::class.java)
        return MongoEventRepository(json, repository as SpringDataEventRepository<EVENT, ID>, eventType)
    }
}
