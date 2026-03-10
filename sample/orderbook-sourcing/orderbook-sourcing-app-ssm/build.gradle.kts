plugins {
	alias(libs.plugins.fixers.kotlin.jvm)
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.kotlin.spring)
	alias(libs.plugins.ksp)
	alias(libs.plugins.kotlin.serialization)
}

dependencies {
	implementation(project(":sample:orderbook-sourcing:orderbook-sourcing-domain"))
	implementation(project(":s2-spring:sourcing:s2-spring-boot-starter-sourcing-ssm"))

	implementation(libs.bundles.spring.redis)

	implementation(libs.f2.spring.starter.function.http)
	implementation(libs.bundles.kserialization.json)
	implementation(libs.arrow.core)
	implementation(libs.arrow.optics)
	ksp(libs.arrow.optics.ksp)

	testImplementation(libs.bundles.testcontainers)

	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.bundles.test.junit)
}
