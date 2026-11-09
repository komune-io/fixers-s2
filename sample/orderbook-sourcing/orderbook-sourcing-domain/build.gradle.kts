plugins {
	alias(catalogue.plugins.fixers.gradle.kotlin.mpp)
	alias(catalogue.plugins.kotlin.serialization)
}

dependencies {
	commonMainApi(project(":s2-automate:s2-automate-dsl"))
	commonMainApi(libs.f2.dsl.function)
	commonMainApi(libs.kotlinx.serialization.core)
}
