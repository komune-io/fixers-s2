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

// Pin patched transitive npm deps of the Kotlin/JS test toolchain (Dependabot alerts on
// kotlin-js-store/yarn.lock). mocha 11.7.6, shipped by KGP, caps them at vulnerable majors.
// Paths are scoped: an unscoped "glob" also upgrades karma's glob 7 and breaks karma.
// Upstream bumps do not make a pin safe to drop (mocha 12 still asks for minimatch ^10, which is
// vulnerable): remove a pin only once a lock regenerated without it still resolves patched versions
// for every advisory. Regenerate the lock with `make -f infra/make/libs.mk js-lock-refresh`.
plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
	the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().apply {
		resolution("**/mocha/glob", "14.0.4")
		resolution("**/mocha/minimatch", "11.0.5")
		resolution("**/karma-webpack/minimatch", "11.0.5")
		resolution("**/mocha/serialize-javascript", "7.1.4")
		resolution("**/mocha/diff", "8.0.4")
	}
}
