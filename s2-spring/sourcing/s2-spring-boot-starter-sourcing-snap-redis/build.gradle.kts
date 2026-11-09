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

tasks.test {
	// Snapshots are written as UTF-8 whatever the JVM's default charset: a non-UTF-8 default proves it.
	jvmArgs("-Dfile.encoding=US-ASCII")
}
