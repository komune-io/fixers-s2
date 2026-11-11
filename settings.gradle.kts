pluginManagement {
    // One fixers version for the build: the catalogue's, which the settings plugin also checks.
    val fixersVersion = settingsDir.resolve("gradle/libs.versions.toml").readLines()
        .firstNotNullOf { Regex("^fixers\\s*=\\s*\"([^\"]+)\"").find(it)?.groupValues?.get(1) }
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") }
    }
    plugins {
        id("io.komune.fixers.gradle.settings") version fixersVersion
    }
}

plugins {
    id("io.komune.fixers.gradle.settings")
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
    "s2-spring:sourcing:s2-spring-boot-starter-sourcing-snap-redis",
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
