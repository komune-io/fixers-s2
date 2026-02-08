plugins {
	id("io.komune.fixers.gradle.kotlin.jvm")
}

dependencies {
	Dependencies.Spring.redis(::api)
	Dependencies.Fixers.f2Http (::api)
	Dependencies.jackson (::api)
	Dependencies.kserializationJson (::api)

	Dependencies.junit(::testImplementation)
	Dependencies.testcontainersRedis(::testImplementation)
}
