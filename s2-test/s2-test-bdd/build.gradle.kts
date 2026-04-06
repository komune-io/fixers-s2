plugins {
	alias(catalogue.plugins.fixers.gradle.kotlin.jvm)
	alias(catalogue.plugins.kotlin.spring)
	alias(catalogue.plugins.fixers.gradle.publish)
}

dependencies {
	implementation(libs.bundles.spring.data.commons)
	implementation(catalogue.spring.boot.starter.auth)

	api(libs.bundles.cucumber)
	api(catalogue.spring.boot.starter.function.http)
	api(project(":s2-automate:s2-automate-core"))
	api(project(":s2-automate:s2-automate-dsl"))
	api(libs.bundles.test.junit)
}
