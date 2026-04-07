package s2.sample.did.http.app

import f2.client.function
import f2.client.ktor.http.httpClientBuilder
import java.util.UUID
import java.util.function.Function
import java.util.function.Supplier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.function.context.FunctionCatalog
import org.springframework.test.context.junit.jupiter.SpringExtension
import s2.sample.did.domain.features.DidCreateCommand
import s2.sample.did.domain.features.DidCreatedEvent

@TestInstance(PER_CLASS)
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class S2DidHttpAppTest {

	@Value("\${local.server.port}")
	private var port: Int = 8080

	@Test
	fun testBasicAggregateFnc() = runBlocking<Unit> {
		val id = UUID.randomUUID().toString()
		val client = httpClientBuilder().build("http://localhost:${port}")
		val result: Flow<DidCreatedEvent> = client.function<DidCreateCommand, DidCreatedEvent>("createDid")
			.invoke(flowOf( DidCreateCommand(
			id = id
		)))
		Assertions.assertThat(result.first().id).isEqualTo(id)
	}


	@Autowired
	lateinit var catalog: FunctionCatalog

	@Test
	fun testCatalogue() = runBlocking {
		val version = catalog.lookup<Any>("version")
		val createDid = catalog.lookup<Any>("createDid")
		val addPublicKey = catalog.lookup<Any>("addPublicKey")
		val revoke = catalog.lookup<Any>("revoke")
		val namesFunction = catalog.getNames(Function::class.java)
		val namesSupplier = catalog.getNames(Supplier::class.java)
		println(version)
		println(createDid)
		println(addPublicKey)
		println(revoke)
		println(namesFunction)
		println(namesSupplier)
	}

}
