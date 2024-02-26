plugins {
	id("io.komune.fixers.gradle.kotlin.jvm")
	id("io.komune.fixers.gradle.publish")
}

dependencies {
	implementation(project(":s2-automate:s2-automate-dsl"))
}
