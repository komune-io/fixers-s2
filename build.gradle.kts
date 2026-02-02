plugins {
	kotlin("plugin.spring") version PluginVersions.kotlin apply false
	kotlin("plugin.serialization") version PluginVersions.kotlin apply false

	kotlin("kapt") version PluginVersions.kotlin apply false

	id("com.github.node-gradle.node") version "7.1.0"

	id("io.komune.fixers.gradle.config") version PluginVersions.fixers
	id("io.komune.fixers.gradle.check") version PluginVersions.fixers
	id("io.komune.fixers.gradle.d2") version PluginVersions.d2

	id("io.komune.fixers.gradle.kotlin.mpp") version PluginVersions.fixers apply false
	id("io.komune.fixers.gradle.kotlin.jvm") version PluginVersions.fixers apply false
	id("io.komune.fixers.gradle.publish") version PluginVersions.fixers apply false
}

allprojects {
	group = "io.komune.s2"
	version = System.getenv("VERSION") ?: "experimental-SNAPSHOT"
	repositories {
		defaultRepo()
	}
}


tasks {
	register<com.github.gradle.node.yarn.task.YarnTask>("installYarn") {
		dependsOn("build")
		args = listOf("install")
	}

	register<com.github.gradle.node.yarn.task.YarnTask>("storybook") {
		dependsOn("yarn_install")
		args = listOf("storybook")
	}
}

fixers {
	d2 {
		outputDirectory = file("storybook/d2/")
	}
	bundle {
		id = "s2"
		name = "S2"
		description = "Fixers S2"
		url = "https://github.com/komune-io/fixers-s2"
	}
	npm {
		organization = "komune"
	}
	sonar {
		organization = "komune-io"
		projectKey = "komune-io_fixers-s2"
	}
}
