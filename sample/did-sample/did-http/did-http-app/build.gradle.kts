plugins {
	alias(catalogue.plugins.kotlin.spring)
	alias(catalogue.plugins.fixers.gradle.kotlin.jvm)
	alias(catalogue.plugins.spring.boot)
}

springBoot {
	buildInfo()
}

dependencies {
	implementation(project(":sample:did-sample:did-app"))

	implementation(catalogue.spring.boot.starter.function.http)
	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.bundles.test.junit)
}
