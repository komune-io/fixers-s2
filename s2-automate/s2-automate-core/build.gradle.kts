plugins {
	id("city.smartb.fixers.gradle.kotlin.mpp")
	id("city.smartb.fixers.gradle.publish")
}

dependencies {
	commonMainApi(project(":s2-automate-storing:s2-automate-storing-dsl"))
}
