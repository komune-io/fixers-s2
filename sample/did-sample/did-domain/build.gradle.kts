plugins {
	id("io.komune.fixers.gradle.kotlin.mpp")
	kotlin("plugin.serialization")
}

dependencies {
	commonMainImplementation(project(":s2-automate:s2-automate-dsl"))
	Dependencies.Fixers.f2ClientKtor(::commonMainApi)

	jvmTestImplementation(project(":s2-automate:s2-automate-documenter"))
}
