plugins {
	id("io.komune.fixers.gradle.kotlin.mpp")
	id("com.google.devtools.ksp") version PluginVersions.ksp
	kotlin("plugin.serialization")
}

dependencies {
	commonMainApi(project(":s2-automate:s2-automate-dsl"))
	Dependencies.arrow (::commonMainApi, ::ksp)
}
