plugins {
	id("io.komune.fixers.gradle.kotlin.jvm")
	id("io.komune.fixers.gradle.publish")
}

dependencies {
	api(project(":s2-automate:s2-automate-dsl"))
	Dependencies.Spring.dataCommons(::api)
}
