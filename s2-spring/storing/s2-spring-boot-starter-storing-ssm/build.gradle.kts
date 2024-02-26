plugins {
	id("io.komune.fixers.gradle.kotlin.jvm")
	id("io.komune.fixers.gradle.publish")
	kotlin("plugin.spring")
}

dependencies {
	api(project(":s2-automate:s2-automate-core"))
	api(project(":s2-spring:storing:s2-spring-boot-starter-storing"))

	api("io.komune.ssm:ssm-data-spring-boot-starter:${Versions.ssm}")
	api("io.komune.ssm:ssm-tx-spring-boot-starter:${Versions.ssm}")
	api("io.komune.ssm:ssm-tx-init-ssm-spring-boot-starter:${Versions.ssm}")
}
