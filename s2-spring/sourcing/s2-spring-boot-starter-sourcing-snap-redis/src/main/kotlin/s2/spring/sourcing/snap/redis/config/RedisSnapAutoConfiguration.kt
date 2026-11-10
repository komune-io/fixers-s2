package s2.spring.sourcing.snap.redis.config

import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import s2.spring.sourcing.snap.redis.RedisIndexInitializer
import s2.spring.sourcing.snap.redis.RedisIndexes

/** Makes every [RedisIndexInitializer]'s index once all beans exist, in their declared order. */
@AutoConfiguration
open class RedisSnapAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    open fun redisIndexes(indexes: ObjectProvider<RedisIndexInitializer>): RedisIndexes =
        RedisIndexes(indexes.orderedStream().toList())
}
