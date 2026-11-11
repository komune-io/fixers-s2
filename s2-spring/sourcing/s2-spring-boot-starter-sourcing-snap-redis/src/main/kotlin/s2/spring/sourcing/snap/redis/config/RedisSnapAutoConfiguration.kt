package s2.spring.sourcing.snap.redis.config

import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import s2.spring.sourcing.snap.redis.IndexSettings
import s2.spring.sourcing.snap.redis.MIN_PREFIX
import s2.spring.sourcing.snap.redis.PhoneticMatcher
import s2.spring.sourcing.snap.redis.RedisIndexInitializer
import s2.spring.sourcing.snap.redis.RedisIndexes

/**
 * Makes every [RedisIndexInitializer]'s index once all beans exist, in their declared order, and provides the
 * [IndexSettings] of `s2.snap.redis.*` to the views that take it.
 */
@AutoConfiguration
@EnableConfigurationProperties(RedisSnapProperties::class)
open class RedisSnapAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    open fun redisIndexes(indexes: ObjectProvider<RedisIndexInitializer>): RedisIndexes =
        RedisIndexes(indexes.orderedStream().toList())

    @Bean
    @ConditionalOnMissingBean
    open fun redisSnapIndexSettings(properties: RedisSnapProperties): IndexSettings =
        IndexSettings(phonetic = properties.phonetic, wildcardMinPrefix = properties.wildcardMinPrefix)
}

/** `s2.snap.redis.*`: how the application's views read text. */
@ConfigurationProperties("s2.snap.redis")
data class RedisSnapProperties(
    /** The phonetic matcher of every TEXT attribute; none by default. */
    val phonetic: PhoneticMatcher? = null,
    /** See [IndexSettings.wildcardMinPrefix]. */
    val wildcardMinPrefix: Int = MIN_PREFIX,
)
