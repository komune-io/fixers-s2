plugins {
	alias(catalogue.plugins.fixers.gradle.kotlin.jvm)
}

dependencies {
	api(libs.bundles.spring.redis)
	api(catalogue.spring.boot.starter.function.http)
	api(libs.jackson.module.kotlin)
	api(libs.kotlinx.serialization.json)

	testImplementation(libs.bundles.test.junit)
	testImplementation(libs.testcontainers.redis)
	testImplementation(libs.bundles.testcontainers)
}
