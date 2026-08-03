package s2.spring.sourcing.data.mongodb.config

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

/**
 * [EnableReactiveMongoRepositories] registers the reactive/coroutine repository
 * infrastructure so the generic [s2.spring.sourcing.data.mongodb.SpringDataEventRepository]
 * built manually by [s2.spring.sourcing.data.mongodb.MongoEventRepositoryFactory] bridges
 * the coroutine CRUD methods (save/findAll/count) to their reactive equivalents.
 */
@EnableReactiveMongoRepositories(basePackages = ["s2.spring.sourcing.data.mongodb"])
@SpringBootApplication
open class TestApplication

fun main(args: Array<String>) {
    runApplication<TestApplication>(*args)
}
