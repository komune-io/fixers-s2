package s2.sample.orderbook.sourcing.app.ssm.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.RedisSerializationContext.RedisSerializationContextBuilder
import org.springframework.data.redis.serializer.StringRedisSerializer
import s2.sample.subautomate.domain.model.OrderBook


@Configuration
class RedisConfig {

	@Bean
	fun reactiveRedisTemplate(
		factory: ReactiveRedisConnectionFactory
	): ReactiveRedisTemplate<String, OrderBook> {
		val keySerializer = StringRedisSerializer()

		val valueSerializer = Jackson2JsonRedisSerializer(jacksonObjectMapper(), OrderBook::class.java)
		val builder: RedisSerializationContextBuilder<String, OrderBook> =
			RedisSerializationContext.newSerializationContext(keySerializer)
		val context: RedisSerializationContext<String, OrderBook> = builder.value(valueSerializer).build()
		return ReactiveRedisTemplate(factory, context)
	}

}
