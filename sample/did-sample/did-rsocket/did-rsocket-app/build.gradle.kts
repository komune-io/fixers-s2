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

	implementation("io.komune.f2:f2-spring-boot-starter-function-rsocket:${Versions.f2}")

	api("io.komune.f2:f2-client-ktor:${Versions.f2}")
	api("io.komune.f2:f2-client-ktor-rsocket:${Versions.f2}")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
}
