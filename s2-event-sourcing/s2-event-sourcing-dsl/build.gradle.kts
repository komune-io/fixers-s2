plugins {
	alias(libs.plugins.fixers.kotlin.mpp)
	alias(libs.plugins.fixers.publish)
}

dependencies {
	commonMainApi(project(":s2-automate:s2-automate-dsl"))
}
