plugins {
	id("io.komune.fixers.gradle.kotlin.mpp")
	id("io.komune.fixers.gradle.publish")

	kotlin("plugin.serialization")
//	id("dev.petuska.npm.publish")
}

dependencies {
	commonMainImplementation(project(":s2-automate:s2-automate-dsl"))
	commonMainApi("io.komune.f2:f2-client-ktor:${Versions.f2}")

	jvmTestImplementation(project(":s2-automate:s2-automate-documenter"))
}

tasks.withType<Test>().configureEach {
	useJUnitPlatform()
}
