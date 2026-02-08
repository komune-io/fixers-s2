plugins {
	id("io.komune.fixers.gradle.kotlin.mpp")
	id("io.komune.fixers.gradle.publish")
}

dependencies {
	commonMainApi(project(":s2-automate:s2-automate-dsl"))
	commonMainApi(project(":s2-event-sourcing:s2-event-sourcing-dsl"))
}
