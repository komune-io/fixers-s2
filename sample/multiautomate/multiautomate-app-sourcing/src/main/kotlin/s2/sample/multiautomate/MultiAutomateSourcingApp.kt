package s2.sample.multiautomate

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

const val PACKAGE = "s2.sample.multiautomate"

@EntityScan(PACKAGE)
@EnableReactiveMongoRepositories(PACKAGE)
@SpringBootApplication(scanBasePackages = [PACKAGE])
class MultiAutomateSourcingApp

// The context must outlive main: closing it would stop the application it just started.
@Suppress("MissingUseCall")
fun main(vararg args: String) {
    runApplication<MultiAutomateSourcingApp>(*args)
}
