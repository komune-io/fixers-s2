plugins {
	alias(libs.plugins.fixers.kotlin.jvm)
	alias(libs.plugins.fixers.publish)
}

dependencies {
	api(project(":s2-automate:s2-automate-dsl"))
	api(libs.bundles.spring.data.commons)
}
