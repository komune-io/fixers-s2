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

fun main(args: Array<String>) {
	runApplication<MultiAutomateSourcingApp>(*args)
}
