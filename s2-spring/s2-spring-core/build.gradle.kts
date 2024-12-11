plugins {
	id("io.komune.fixers.gradle.kotlin.jvm")
	id("io.komune.fixers.gradle.publish")
	kotlin("kapt")
}

dependencies {
	api(project(":s2-automate:s2-automate-core"))
	Dependencies.Spring.autoConfigure(::implementation, ::kapt)

	compileOnly("io.komune.c2:ssm-chaincode-dsl:${Versions.c2}")
}
