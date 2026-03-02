plugins {
	`kotlin-dsl`
}

repositories {
	if(System.getenv("MAVEN_LOCAL_USE") == "true") {
		mavenLocal()
	}
	mavenCentral()
	maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") }
}

val fixersVersion = "0.27.3"

dependencies {
	implementation("io.komune.fixers.gradle:dependencies:$fixersVersion")
}
