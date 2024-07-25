VERSION = $(shell cat VERSION)

.PHONY: lint build test publish promote version

lint:
	./gradlew detekt

build:
	./gradlew :sample:did-sample:did-domain:dependencies
	VERSION=$(VERSION) ./gradlew clean build publishToMavenLocal -x test --refresh-dependencies

test-pre:
	make dev up
	make dev bclan-init logs
	make dev up

test:
	./gradlew test

publish:
	VERSION=$(VERSION) PKG_MAVEN_REPO=github ./gradlew publish

promote:
	VERSION=$(VERSION) PKG_MAVEN_REPO=sonatype_oss ./gradlew publish

.PHONY: version
version:
	@echo "$(VERSION)"
