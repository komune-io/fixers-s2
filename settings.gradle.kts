rootProject.name = "fixers-s2"

pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()
		maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots") }
	}
}

include(
	"s2-automate:s2-automate-core",
	"s2-automate:s2-automate-documenter",
	"s2-automate:s2-automate-dsl"
)

include(
	"s2-event-sourcing:s2-event-sourcing-dsl",
)
include(
	"s2-test:s2-test-bdd",
)

include(
	"s2-spring:s2-spring-core"
)

include(
	"s2-spring:storing:s2-spring-boot-starter-storing",
	"s2-spring:storing:s2-spring-boot-starter-storing-data",
	"s2-spring:storing:s2-spring-boot-starter-storing-ssm"
)

include(
	"s2-spring:sourcing:s2-spring-boot-starter-sourcing",
	"s2-spring:sourcing:s2-spring-boot-starter-sourcing-data",
	"s2-spring:sourcing:s2-spring-boot-starter-sourcing-data-mongodb",
	"s2-spring:sourcing:s2-spring-boot-starter-sourcing-data-r2dbc",
	"s2-spring:sourcing:s2-spring-boot-starter-sourcing-ssm",
)

include(
	"s2-spring:utils:s2-spring-boot-starter-utils-data",
	"s2-spring:utils:s2-spring-boot-starter-utils-logger"
)

include(
	"sample:multiautomate",
	"sample:multiautomate:multiautomate-app-sourcing",
	"sample:multiautomate:multiautomate-app-storing"
)

include(
	"sample:orderbook-sourcing",
	"sample:orderbook-sourcing:orderbook-sourcing-app-mongodb",
	"sample:orderbook-sourcing:orderbook-sourcing-app-r2dbc",
	"sample:orderbook-sourcing:orderbook-sourcing-app-ssm",
	"sample:orderbook-sourcing:orderbook-sourcing-app-ssm-permissive",
	"sample:orderbook-sourcing:orderbook-sourcing-domain",

	"sample:orderbook-storing:orderbook-storing-app-ssm"
)

include(
	"sample:did-sample",
	"sample:did-sample:did-app",
	"sample:did-sample:did-http:did-http-app",
	"sample:did-sample:did-rsocket:did-rsocket-app",
	"sample:did-sample:did-domain",
	"sample:did-sample:did-ui"
)
