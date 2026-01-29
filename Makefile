VERSION = $(shell cat VERSION)

.PHONY: lint build test publish promote

## New
lint:
	@make -f infra/make/libs.mk lint
	@make -f infra/make/docs.mk lint

build:
	@make -f infra/make/libs.mk build
	@make -f infra/make/docs.mk build

test-pre:
	@make -f infra/make/libs.mk test-pre

test:
	@make -f infra/make/libs.mk test
	@make -f infra/make/docs.mk test

stage:
	@make -f infra/make/libs.mk stage
	@make -f infra/make/docs.mk stage

promote:
	@make -f infra/make/libs.mk promote
	@make -f infra/make/docs.mk promote

## DEV ENVIRONMENT
include infra/docker-compose/dev-compose.mk
