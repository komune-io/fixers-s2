import io.komune.fixers.gradle.dependencies.FixersDependencies
import io.komune.fixers.gradle.dependencies.FixersPluginVersions
import io.komune.fixers.gradle.dependencies.FixersVersions
import io.komune.fixers.gradle.dependencies.Scope
import io.komune.fixers.gradle.dependencies.add
import java.net.URI
import org.gradle.api.artifacts.dsl.RepositoryHandler

object PluginVersions {
	val fixers = FixersPluginVersions.fixers
	val d2 = FixersPluginVersions.fixers
	const val ksp = FixersPluginVersions.ksp
	const val kotlin = FixersPluginVersions.kotlin
	const val springBoot = FixersPluginVersions.springBoot
}

object Versions {
	const val arrow = "2.2.1.1"

	const val springBoot = FixersVersions.Spring.boot
	const val springframework = FixersVersions.Spring.framework
	object Testcontainers {
        const val postgres = "1.21.4"
        const val mongo = "1.21.4"
        const val junitJupiter = "1.21.4"
        const val r2dbc = "1.21.4"
        const val redis = "2.2.4"
	}
	val c2 = FixersPluginVersions.fixers
	val f2 = FixersPluginVersions.fixers
	val slf4j = FixersVersions.Logging.slf4j

	val commonsPool = "2.13.1"
	const val postgresql = "42.7.3"
	const val r2dbc = "1.1.1.RELEASE"
	const val lettuce = "7.3.0.RELEASE"
}

fun RepositoryHandler.defaultRepo() {
	if(System.getenv("MAVEN_LOCAL_USE") == "true") {
		mavenLocal()
	}
	mavenCentral()
	maven { url = URI("https://central.sonatype.com/repository/maven-snapshots") }
}

object Dependencies {
	fun kserializationJson(scope: Scope) = FixersDependencies.Jvm.Json.kSerialization(scope)

	fun slf4j(scope: Scope) = scope.add(
		"org.slf4j:slf4j-api",
	)

	object Fixers {
		fun f2Http(scope: Scope) = scope.add(
			"io.komune.f2:f2-spring-boot-starter-function-http",
		)
		fun f2Auth(scope: Scope) = scope.add(
			"io.komune.f2:f2-spring-boot-starter-auth",
		)
		fun f2ClientKtor(scope: Scope) = scope.add(
			"io.komune.f2:f2-client-ktor",
		)
	}

	object C2 {
		fun ssmChaincodeDsl(scope: Scope) = scope.add(
			"io.komune.c2:ssm-chaincode-dsl:${Versions.c2}",
		)
		fun ssmSourcing(scope: Scope) = scope.add(
			"io.komune.c2:ssm-chaincode-spring-boot-starter:${Versions.c2}",
			"io.komune.c2:ssm-data-spring-boot-starter:${Versions.c2}",
			"io.komune.c2:ssm-tx-spring-boot-starter:${Versions.c2}",
		)
		fun ssmStoring(scope: Scope) = scope.add(
			"io.komune.c2:ssm-chaincode-spring-boot-starter:${Versions.c2}",
			"io.komune.c2:ssm-tx-spring-boot-starter:${Versions.c2}",
			"io.komune.c2:ssm-tx-init-ssm-spring-boot-starter:${Versions.c2}",
		)
	}


	fun arrow(scope: Scope, ksp: Scope) = scope.add(
		"io.arrow-kt:arrow-core",
		"io.arrow-kt:arrow-optics",
	).also {
		ksp.add(
			"io.arrow-kt:arrow-optics-ksp-plugin"
		)
	}

	object Spring {
		fun dataCommons(scope: Scope) = FixersDependencies.Jvm.Spring.dataCommons(scope)
		fun autoConfigure(scope: Scope, ksp: Scope) = FixersDependencies.Jvm.Spring.autoConfigure(scope, ksp)
		fun security(scope: Scope) = scope.add(
				"org.springframework.boot:spring-boot-starter-security"
		)

		fun redis(scope: Scope) = scope.add(
			"org.springframework.boot:spring-boot-starter-data-redis-reactive",
			"io.lettuce:lettuce-core:${Versions.lettuce}",
			"com.fasterxml.jackson.core:jackson-databind:2.18.3",
			"org.apache.commons:commons-pool2:${Versions.commonsPool}",
		)

		fun mongo(scope: Scope) = scope.add(
			"org.springframework.boot:spring-boot-starter-data-mongodb-reactive"
		)

		fun r2dbc(scope: Scope) = scope.add(
			"org.springframework.boot:spring-boot-starter-data-r2dbc",
		)

		fun tx(scope: Scope) = scope.add(
			"org.springframework:spring-tx"
		)
	}

	fun testcontainers(scope: Scope) = scope.add(
		"org.springframework.boot:spring-boot-testcontainers",
		"org.testcontainers:junit-jupiter:${Versions.Testcontainers.junitJupiter}",
		"org.testcontainers:mongodb:${Versions.Testcontainers.mongo}",
	)


	fun testcontainersPostgres(scope: Scope, runtimeOnly: Scope) = scope.add(
		"org.testcontainers:postgresql:${Versions.Testcontainers.postgres}",
		"org.testcontainers:r2dbc:${Versions.Testcontainers.r2dbc}",
	).also { testcontainers(scope) }
		.also {
			runtimeOnly.add(
				"org.postgresql:postgresql:${Versions.postgresql}",
				"org.postgresql:r2dbc-postgresql:${Versions.r2dbc}"
			)
		}
	fun testcontainersRedis(scope: Scope) = scope.add(
		"com.redis:testcontainers-redis:${Versions.Testcontainers.redis}",
	).also { testcontainers(scope) }

	fun springTest(scope: Scope) = scope.add(
		"org.springframework.boot:spring-boot-starter-test",
	).also {
		junit(scope)
	}

	fun jackson(scope: Scope) = FixersDependencies.Jvm.Json.jackson(scope)
	fun junit(scope: Scope) = FixersDependencies.Jvm.Test.junit(scope)
	fun cucumber(scope: Scope) = FixersDependencies.Jvm.Test.cucumber(scope)
}
