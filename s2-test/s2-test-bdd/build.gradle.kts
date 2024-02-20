plugins {
	id("io.komune.fixers.gradle.kotlin.jvm")
	kotlin("plugin.spring")
	id("io.komune.fixers.gradle.publish")
}

dependencies {
	Dependencies.Fixers.i2(::api)

	Dependencies.Spring.dataCommons(::implementation)

	Dependencies.cucumber(::api)
	Dependencies.Fixers.f2Http(::api)
	api(project(":s2-automate:s2-automate-core"))
	api(project(":s2-automate:s2-automate-dsl"))
	Dependencies.junit(::api)
}
