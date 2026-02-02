plugins {
	kotlin("plugin.spring")
	id("io.komune.fixers.gradle.kotlin.jvm")
	id("org.springframework.boot")
}

springBoot {
	buildInfo()
}

dependencies {
	implementation(project(":sample:did-sample:did-app"))

	Dependencies.Fixers.f2Http(::implementation)
	Dependencies.springTest(::testImplementation)
}
