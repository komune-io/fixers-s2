plugins {
	alias(libs.plugins.fixers.kotlin.jvm)
	alias(libs.plugins.kotlin.spring)
	alias(libs.plugins.fixers.publish)
}

dependencies {

	implementation(libs.bundles.spring.data.commons)
	implementation(libs.f2.spring.starter.auth)

	api(libs.bundles.cucumber)
	api(libs.f2.spring.starter.function.http)
	api(project(":s2-automate:s2-automate-core"))
	api(project(":s2-automate:s2-automate-dsl"))
	api(libs.bundles.test.junit)
}
