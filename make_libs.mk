VERSION = $(shell cat VERSION)

.PHONY: lint build test publish promote version

lint:
	./gradlew detekt

build:
	VERSION=$(VERSION) ./gradlew clean build publishToMavenLocal -x test

test-pre:
	make dev up
	make dev bclan-init logs
	make dev up

test:
	./gradlew test

state:
	VERSION=$(VERSION) ./gradlew state

promote:
	VERSION=$(VERSION) ./gradlew promote

.PHONY: version
version:
	@echo "$(VERSION)"
