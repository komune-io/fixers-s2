VERSION = $(shell cat VERSION)

.PHONY: clean lint build test publish promote version

clean:
	./gradlew clean
	rm -rf node_modules

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

stage:
	VERSION=$(VERSION) ./gradlew stage

promote:
	VERSION=$(VERSION) ./gradlew promote

.PHONY: version
version:
	@echo "$(VERSION)"
