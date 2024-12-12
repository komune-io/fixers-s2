
plugins {
	id("io.komune.fixers.gradle.kotlin.mpp")
	id("io.komune.fixers.gradle.publish")
	kotlin("plugin.serialization")
}

dependencies {
    commonMainApi("io.komune.c2:ssm-chaincode-dsl:${Versions.c2}")
}
