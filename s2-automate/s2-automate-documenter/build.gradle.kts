plugins {
	alias(libs.plugins.fixers.kotlin.jvm)
	alias(libs.plugins.fixers.publish)
}

dependencies {
	implementation(project(":s2-automate:s2-automate-dsl"))
}
