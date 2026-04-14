VERSION = $(shell cat VERSION)

.PHONY: clean lint build test publish promote

clean:
	@make -f infra/make/libs.mk clean

lint:
	@make -f infra/make/libs.mk lint

build:
	@make -f infra/make/libs.mk build

test:
	@make -f infra/make/libs.mk test

stage:
	@make -f infra/make/libs.mk stage

promote:
	@make -f infra/make/libs.mk promote
