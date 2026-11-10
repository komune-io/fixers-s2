plugins {
	alias(catalogue.plugins.fixers.gradle.kotlin.jvm)
	alias(catalogue.plugins.kotlin.serialization)
}

dependencies {
	api(project(":s2-automate:s2-automate-dsl"))
	api(libs.f2.dsl.function)
	api(libs.kotlinx.serialization.core)
}
