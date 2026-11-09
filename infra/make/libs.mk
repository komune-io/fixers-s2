VERSION = $(shell cat VERSION)

.PHONY: clean lint build test check publish promote version verify-metadata verify-metadata-dry js-lock-refresh

clean:
	./gradlew clean

lint:
	./gradlew detekt

build:
	VERSION=$(VERSION) ./gradlew clean build publishToMavenLocal -x test -x jvmTest -x jsTest -x jsBrowserTest

test:
	./gradlew allTests test

check:
	VERSION=$(VERSION) ./gradlew sonar

stage:
	VERSION=$(VERSION) ./gradlew stage

promote:
	VERSION=$(VERSION) ./gradlew promote

verify-metadata: verify-metadata-dry
	mv gradle/verification-metadata.dryrun.xml gradle/verification-metadata.xml
	mv gradle/verification-keyring.dryrun.keys gradle/verification-keyring.keys
	mv gradle/verification-keyring.dryrun.gpg gradle/verification-keyring.gpg

# Generates the same files with the .dryrun suffix, to inspect the delta without replacing anything.
verify-metadata-dry:
	./gradlew --write-verification-metadata pgp,sha256 --export-keys --dry-run build publishToMavenLocal

# Regenerates kotlin-js-store/yarn.lock from scratch (the build sets no yarn resolutions).
js-lock-refresh:
	rm -rf build/js kotlin-js-store/yarn.lock
	./gradlew rootPackageJson --rerun kotlinUpgradeYarnLock

.PHONY: version
version:
	@echo "$(VERSION)"
