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
	const val arrow = "1.0.2-alpha.42"

	const val springBoot = FixersVersions.Spring.boot
	const val springframework = FixersVersions.Spring.framework
	const val testcontainers = FixersVersions.Test.testcontainers
	object Testcontainers {
		val postgres = "1.21.4"
		val mongo = "1.21.4"
		val junitJupiter = "1.21.4"
		val r2dbc = "1.21.4"
	}
	val c2 = FixersPluginVersions.fixers
	val f2 = FixersPluginVersions.fixers
	val slf4j = FixersVersions.Logging.slf4j

	const val postgresql = "42.7.3"
	const val r2dbc = "1.0.7.RELEASE"
	const val redisSpring = "4.4.0"
	const val redisTestContainer = "2.2.4"
	const val lettuce = "6.7.1.RELEASE"
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

	object Fixers {
		fun f2Http(scope: Scope) = scope.add(
			"io.komune.f2:f2-spring-boot-starter-function-http:${Versions.f2}",
		)fun f2Auth(scope: Scope) = scope.add(
			"io.komune.f2:f2-spring-boot-starter-auth:${Versions.f2}",
		)
	}


	fun arrow(scope: Scope, ksp: Scope) = scope.add(
		"io.arrow-kt:arrow-core:${Versions.arrow}",
		"io.arrow-kt:arrow-optics:${Versions.arrow}",
	).also {
		ksp.add(
			"io.arrow-kt:arrow-optics-ksp-plugin:${Versions.arrow}"
		)
	}

	object Spring {
		fun dataCommons(scope: Scope) = FixersDependencies.Jvm.Spring.dataCommons(scope)
		fun autoConfigure(scope: Scope, ksp: Scope) = FixersDependencies.Jvm.Spring.autoConfigure(scope, ksp)
		fun security(scope: Scope) = scope.add(
				"org.springframework.boot:spring-boot-starter-security:${FixersVersions.Spring.security}"
		)

		fun redis(scope: Scope) = scope.add(
			"org.springframework.boot:spring-boot-starter-data-redis-reactive:${Versions.springBoot}",
			"com.redis:lettucemod:${Versions.redisSpring}",
			"io.lettuce:lettuce-core:${Versions.lettuce}",
			"org.apache.commons:commons-pool2:2.12.0",
			"com.fasterxml.jackson.core:jackson-databind:2.18.3"
		)

		fun mongo(scope: Scope) = scope.add(
			"org.springframework.boot:spring-boot-starter-data-mongodb-reactive:${Versions.springBoot}"
		)

		fun r2dbc(scope: Scope) = scope.add(
			"org.springframework.boot:spring-boot-starter-data-r2dbc:${Versions.springBoot}",
		)

		fun tx(scope: Scope) = scope.add(
			"org.springframework:spring-tx:${Versions.springframework}"
		)
	}

	fun testcontainers(scope: Scope) = scope.add(
		"org.springframework.boot:spring-boot-testcontainers:${Versions.springBoot}",
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
		"com.redis:testcontainers-redis:${Versions.redisTestContainer}",
	).also { testcontainers(scope) }

	fun springTest(scope: Scope) = scope.add(
		"org.springframework.boot:spring-boot-starter-test:${Versions.springBoot}",
	).also {
		junit(scope)
	}

	fun jackson(scope: Scope) = FixersDependencies.Jvm.Json.jackson(scope)
	fun junit(scope: Scope) = FixersDependencies.Jvm.Test.junit(scope)
	fun cucumber(scope: Scope) = FixersDependencies.Jvm.Test.cucumber(scope)
}
