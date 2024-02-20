plugins {
	id("io.spring.dependency-management")
	kotlin("plugin.spring")
	id("io.komune.fixers.gradle.kotlin.jvm")
	id("org.springframework.boot")
}

springBoot {
	buildInfo()
}

dependencies {
	implementation(project(":sample:did-sample:did-app"))
	//TODO THis should not be needed
	implementation ("com.google.code.gson:gson:2.8.9")

	Dependencies.Fixers.f2Http(::implementation)
	Dependencies.springTest(::testImplementation)
}
