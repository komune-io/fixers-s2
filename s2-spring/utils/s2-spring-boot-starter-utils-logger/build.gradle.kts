plugins {
	id("io.komune.fixers.gradle.kotlin.jvm")
	id("io.komune.fixers.gradle.publish")
}

dependencies {
	api("org.slf4j:slf4j-api:${Versions.slf4j}")
}
