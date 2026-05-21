plugins {
	alias(catalogue.plugins.fixers.gradle.kotlin.mpp)
	alias(catalogue.plugins.fixers.gradle.publish)
}

dependencies {
	commonMainApi(project(":s2-automate:s2-automate-dsl"))
	commonMainApi(project(":s2-event-sourcing:s2-event-sourcing-dsl"))

	commonTestImplementation(kotlin("test"))
	"jvmTestImplementation"(kotlin("test-junit5"))
	"jsTestImplementation"(kotlin("test-js"))
}
