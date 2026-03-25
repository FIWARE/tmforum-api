# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FIWARE implementation of TMForum APIs, backed by NGSI-LD context brokers (Orion-LD or Scorpio). Maven multi-module project with Java 17, Micronaut 3.9.1, and reactive programming (Project Reactor).

## Build Commands

```bash
# Full build (skipping tests)
mvn clean install -DskipTests -DskipITs

# Unit tests only
mvn test

# Integration tests (spins up K3S cluster with broker + MongoDB automatically)
mvn integration-test

# Build + test a single module
mvn test -pl product-catalog
mvn integration-test -pl product-catalog

# Run a single test class
mvn test -pl product-catalog -Dtest=ProductOfferingTest

# Run a single integration test class
mvn integration-test -pl product-catalog -Dit.test=ProductOfferingApiIT -DskipTests

# Dev environment (persistent K3S cluster, broker at localhost:1026)
mvn install -Pdev
# Cleanup: mvn clean k3s:rm -Pdev

# Conformance tests (one module at a time)
mvn clean install -Pconformance-test --projects common,mapping,<MODULE> -DskipTests -DskipITs

# Build OCI images (multi-arch)
mvn install -Poci -DskipTests -DskipITs

# Static analysis
mvn spotbugs:spotbugs
```

## Architecture

### Module Structure

- **common**: Shared base classes, repositories, controllers, domain objects, utilities. All API modules depend on this.
- **\*-shared-models** (customer, resource, product, service): Domain objects shared across multiple API modules.
- **API modules** (18 total): Each implements one TMForum API (e.g., `product-catalog`, `party-catalog`, `customer-management`). Each produces a standalone JAR/container.
- **all-in-one**: Single uber-JAR combining all API modules.
- **data-migrator**: Migration tool with 3 sub-modules (legacy-loader, update-writer, migrator).

### Layered Architecture (per API module)

1. **REST Controllers**: Generated from OpenAPI specs via OpenAPI Generator, then extended. Base class: `AbstractApiController<T>` in common.
2. **Domain Models**: Hand-written domain classes. Generated Value Objects (VOs, suffixed `VO`) from OpenAPI specs. MapStruct mappers convert between them.
3. **Repository Layer**: `TmForumRepository` interface → `NgsiLdBaseRepository` abstract implementation → module-specific repositories. All operations are reactive (return `Mono`/`Flux`).
4. **NGSI-LD Backend**: Entities are stored as NGSI-LD entities in a context broker. The `ngsi-ld-java-mapping` library provides annotation-based mapping between Java POJOs and NGSI-LD.

### Code Generation

OpenAPI specs are downloaded at build time and fed to the OpenAPI Generator Maven Plugin, which produces:
- API interfaces (Micronaut controllers)
- Model classes (suffixed `VO`, using Lombok, extending `UnknownPreservingBase`)
- Test example builders (pre-filled from spec examples)
- Test interfaces defining all endpoint/response test combinations

Generated code lives in `target/` — never edit it directly. To customize behavior, extend the generated interfaces in `src/main/java`.

### Event System

`TMForumEventHandler` handles pub/sub notifications. Each module provides an event mapper (e.g., `ProductCatalogEventMapper`).

### Configuration

Each module has `application.yaml` in `src/main/resources/`. Profiles select the broker and cache:
- `orion-ld` / `scorpio` — broker backend
- `in-memory` / `redis` — cache strategy

Environment variables for tests: `broker` (orion-ld|scorpio), `cache` (in-memory|redis).

## Testing

- **Unit tests** (`*Test.java`): JUnit5 + Mockito. No external dependencies. Run with `mvn test`.
- **Integration tests** (`*IT.java`): JUnit5 + Micronaut-Test + K3S. Require broker at localhost:1026. Base class: `AbstractApiIT`. Run with `mvn integration-test`.
- **Conformance tests**: TMForum CTK in Docker. Run per-module with `-Pconformance-test`.

Integration tests auto-provision a K3S cluster (Orion-LD + MongoDB). For IDE-based runs, first set up a persistent dev environment with `mvn install -Pdev`.

## Key Conventions

- All repository/controller operations are reactive — use `Mono<T>` and `Flux<T>` return types.
- Domain objects map to/from generated VOs via MapStruct mappers. Mapper interfaces live alongside domain classes.
- Validation uses `ReferenceValidationService` for referential integrity and `@schemaLocation` for JSON Schema-based extension validation.
- Container images are published to `quay.io/fiware/tmforum-<module>`.
