plugins {
	alias(catalogue.plugins.kotlin.spring) apply false
	alias(catalogue.plugins.kotlin.serialization) apply false
	alias(catalogue.plugins.kotlin.kapt) apply false
	alias(libs.plugins.node.gradle)

	alias(catalogue.plugins.f2.bom)
	alias(catalogue.plugins.fixers.gradle.config)
	alias(catalogue.plugins.fixers.gradle.check)

	alias(catalogue.plugins.fixers.gradle.kotlin.mpp) apply false
	alias(catalogue.plugins.fixers.gradle.kotlin.jvm) apply false
	alias(catalogue.plugins.fixers.gradle.publish)
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
	bundle {
		group = "io.komune.s2"
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
		properties {
			// Samples are standalone demo applications, not library code:
			// exclude them from coverage and duplication analysis.
			property("sonar.coverage.exclusions", "**/sample/**")
			property("sonar.cpd.exclusions", "**/sample/**")
		}
	}
	repositories {
		sonatypeSnapshots = true
	}
}

// Pin patched transitive npm deps of the Kotlin/JS test toolchain. Versions, and why the pins
// exist and when to remove them, live with the npm-* entries in gradle/libs.versions.toml.
// Paths are scoped: an unscoped "glob" also upgrades karma's glob 7 and breaks karma.
plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
	the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().apply {
		resolution("**/mocha/glob", libs.versions.npm.mocha.glob.get())
		resolution("**/mocha/minimatch", libs.versions.npm.mocha.minimatch.get())
		resolution("**/karma-webpack/minimatch", libs.versions.npm.karma.webpack.minimatch.get())
		resolution("**/mocha/serialize-javascript", libs.versions.npm.mocha.serialize.javascript.get())
		resolution("**/mocha/diff", libs.versions.npm.mocha.diff.get())
	}
}
