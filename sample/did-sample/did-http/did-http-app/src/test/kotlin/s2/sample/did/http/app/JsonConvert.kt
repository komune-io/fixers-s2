package s2.sample.did.http.app

import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import s2.sample.did.domain.features.DidAddPublicKeyCommand

@ExtendWith(SpringExtension::class)
@SpringBootTest
class JsonConvert {

	@Autowired
	lateinit var objectMapper: ObjectMapper

	@Test
	fun testConvertJson() {
		val json = "{\"id\":\"67e40c23-39f0-49b9-be7d-639b35ddad42\", \"publicKey\": \"puubbkey\"}"
		val obj = objectMapper.readValue<DidAddPublicKeyCommand>(json)
		println(obj)
	}

}
