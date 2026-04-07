plugins {
	alias(catalogue.plugins.fixers.gradle.kotlin.jvm)
	alias(catalogue.plugins.fixers.gradle.publish)
}

dependencies {
	api(project(":s2-automate:s2-automate-dsl"))
	api(libs.bundles.spring.data.commons)
}
