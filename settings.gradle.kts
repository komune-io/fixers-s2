pluginManagement {
	repositories {
		if(System.getenv("FIXERS_REPOSITORIES_MAVEN_LOCAL") == "true") {
			mavenLocal()
		}
		gradlePluginPortal()
		mavenCentral()
		maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") }
	}
}

dependencyResolutionManagement {
	repositories {
		mavenCentral()
		maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") }
		if(System.getenv("FIXERS_REPOSITORIES_MAVEN_LOCAL") == "true") {
			mavenLocal()
		}
	}
	versionCatalogs {
		val fixersVersion = file("gradle/libs.versions.toml")
			.readLines()
			.firstNotNullOfOrNull {
				Regex("^fixers\\s*=\\s*\"([^\"]+)\"").find(it)?.groupValues?.get(1)
			} ?: error("fixers version not found in gradle/libs.versions.toml")
		create("catalogue") {
			from("io.komune.f2:f2-gradle-catalog:$fixersVersion")
		}
	}
}

rootProject.name = "fixers-s2"

include(
	"s2-automate:s2-automate-core",
	"s2-automate:s2-automate-documenter",
	"s2-automate:s2-automate-dsl"
)

include(
	"s2-event-sourcing:s2-event-sourcing-dsl",
)
include(
	"s2-test:s2-test-bdd",
)

include(
	"s2-spring:s2-spring-core"
)

include(
	"s2-spring:storing:s2-spring-boot-starter-storing",
	"s2-spring:storing:s2-spring-boot-starter-storing-data",
)

include(
	"s2-spring:sourcing:s2-spring-boot-starter-sourcing",
	"s2-spring:sourcing:s2-spring-boot-starter-sourcing-data",
	"s2-spring:sourcing:s2-spring-boot-starter-sourcing-data-mongodb",
	"s2-spring:sourcing:s2-spring-boot-starter-sourcing-data-r2dbc",
)

include(
	"s2-spring:utils:s2-spring-boot-starter-utils-data",
	"s2-spring:utils:s2-spring-boot-starter-utils-logger"
)

include(
	"sample:multiautomate",
	"sample:multiautomate:multiautomate-app-sourcing",
	"sample:multiautomate:multiautomate-app-storing"
)

include(
	"sample:orderbook-sourcing",
	"sample:orderbook-sourcing:orderbook-sourcing-core",
	"sample:orderbook-sourcing:orderbook-sourcing-app-mongodb",
	"sample:orderbook-sourcing:orderbook-sourcing-app-r2dbc",
	"sample:orderbook-sourcing:orderbook-sourcing-domain",
)
