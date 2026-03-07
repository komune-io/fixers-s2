plugins {
	alias(libs.plugins.fixers.kotlin.jvm)
}

dependencies {
	api(libs.bundles.spring.redis)
	api(libs.f2.spring.starter.function.http)
	api(libs.jackson.module.kotlin)
	api(libs.bundles.kserialization.json)

	testImplementation(libs.bundles.test.junit)
	testImplementation(libs.bundles.testcontainers)
	testImplementation(libs.testcontainers.redis)
}
