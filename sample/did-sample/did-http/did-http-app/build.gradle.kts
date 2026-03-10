plugins {
	alias(libs.plugins.kotlin.spring)
	alias(libs.plugins.fixers.kotlin.jvm)
	alias(libs.plugins.spring.boot)
}

springBoot {
	buildInfo()
}

dependencies {
	implementation(project(":sample:did-sample:did-app"))

	implementation(libs.f2.spring.starter.function.http)
	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.bundles.test.junit)
}
