plugins {
	alias(catalogue.plugins.fixers.gradle.kotlin.jvm)
	alias(catalogue.plugins.spring.boot)
	alias(catalogue.plugins.kotlin.spring)
	alias(catalogue.plugins.ksp)
	alias(catalogue.plugins.kotlin.serialization)
}

dependencies {
	implementation(project(":sample:orderbook-sourcing:orderbook-sourcing-domain"))
	implementation(project(":s2-spring:storing:s2-spring-boot-starter-storing-ssm"))

	implementation(catalogue.spring.boot.starter.function.http)
	implementation(libs.kotlinx.serialization.json)
	implementation(libs.arrow.core)
	implementation(libs.arrow.optics)
	ksp(libs.arrow.optics.ksp)

	testImplementation(libs.bundles.testcontainers)
	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.bundles.test.junit)
}
