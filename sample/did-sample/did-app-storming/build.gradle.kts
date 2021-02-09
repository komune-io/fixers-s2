plugins {
    id("io.spring.dependency-management")
    kotlin("plugin.spring")
    kotlin("jvm")
}

dependencies {

    api(project(":sample:did-sample:did-domain"))

    api(project(":s2-spring:aggregate:s2-spring-boot-starter-aggregate-storming"))


    api("city.smartb.f2:f2-spring-data-mongodb:${Versions.f2}")
    api("city.smartb.f2:f2-spring-boot-starter-function:${Versions.f2}")

    api("city.smartb.f2:f2-feature-version:${Versions.f2}")
    api("city.smartb.f2:f2-feature-catalog:${Versions.f2}")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
}