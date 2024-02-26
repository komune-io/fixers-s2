plugins {
	id("io.komune.fixers.gradle.kotlin.mpp")
	id("io.komune.fixers.gradle.publish")

//	id("dev.petuska.npm.publish")
}

dependencies {
//    jsApi("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:${Versions.coroutines}")
	commonMainApi(project(":s2-automate:s2-automate-dsl"))
}
