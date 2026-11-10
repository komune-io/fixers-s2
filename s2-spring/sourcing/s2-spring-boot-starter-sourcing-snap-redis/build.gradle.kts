plugins {
    alias(catalogue.plugins.fixers.gradle.kotlin.jvm)
    alias(catalogue.plugins.fixers.gradle.publish)
}

dependencies {
    api(project(":s2-automate:s2-automate-core"))
    api(libs.f2.dsl.cqrs)
    api(libs.lettuce.core)
    api(libs.jackson.module.kotlin)
    api(libs.spring.context)
    implementation(libs.spring.boot.autoconfigure)
    implementation(libs.kotlinx.coroutines.reactive)
    implementation(libs.kotlinx.coroutines.reactor)
    implementation(libs.slf4j.api)
    // Lettuce parses JSON.GET and JSON.MGET answers with Jackson 2, an optional dependency of its own.
    runtimeOnly(libs.jackson.databind)

    testImplementation(libs.bundles.test.junit)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.logback.classic)
}

// The suite runs against every Redis Stack the module supports: the one BoisLocal runs in production, and the
// current one, whose RediSearch answers differently (RESP3 warnings, FT.INFO fields).
val redisStacks = mapOf(
    "test" to "redis/redis-stack:6.2.2-v2-edge@sha256:eb0bd59d722b089875c878e88b92bd6de79033242b1e4da8b6e8d789fc284df1",
    "testRedisStack74" to "redis/redis-stack:7.4.0-v8@sha256:5d5154123426693540ea6c1d9e638eae2bf2024879c839db6fcfdd7717e817c7",
)

val testRedisStack74 by tasks.registering(Test::class) {
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
}

tasks.withType<Test>().configureEach {
    // Snapshots are written as UTF-8 whatever the JVM's default charset: a non-UTF-8 default proves it.
    jvmArgs("-Dfile.encoding=US-ASCII")
    systemProperty("s2.redis.image", redisStacks.getValue(name))
}

tasks.check {
    dependsOn(testRedisStack74)
}
