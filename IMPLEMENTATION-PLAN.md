# Implementation Plan: Three New TMForum API Modules

## Context

The project needs three new TMForum API implementations added as Maven modules:
1. **Resource Order Management** (TMF652) — ordering resources, very close analog to product-ordering-management
2. **Service Order Management** (TMF641) — ordering services, very close analog to product-ordering-management
3. **Software Management** (TMF730) — managing software/compute entities and their specifications, closer to resource-catalog/resource-inventory pattern

All three follow existing module patterns: OpenAPI code generation, domain model with NGSI-LD annotations, MapStruct mappers, reactive controllers, event subscriptions, and integration tests.

---

## Module 1: Resource Order Management (TMF652)

**Module directory:** `resource-order-management/`
**Package:** `org.fiware.tmforum.resourceordering`
**Generated packages:** `org.fiware.resourceordering.api` / `org.fiware.resourceordering.model`
**API base path:** `/tmf-api/resourceOrderingManagement/v4/`
**Spec:** `api/tm-forum/resource-order-management/api.json`
**Reference module:** `product-ordering-management/` (closest analog)

### Files to create

#### Build config
- `resource-order-management/pom.xml`
  - Dependencies: common, resource-shared-models
  - OpenAPI generator pointing to `api/tm-forum/resource-order-management/api.json`
  - apiPackage: `org.fiware.resourceordering.api`, modelPackage: `org.fiware.resourceordering.model`

#### Domain classes (`src/main/java/org/fiware/tmforum/resourceordering/domain/`)
- **ResourceOrder.java** — Main aggregate, extends EntityWithId, `@MappingEnabled(entityType = "resource-order")`
  - Properties: href, cancellationDate, cancellationReason, category, completionDate, description, expectedCompletionDate, externalId, name, orderDate, orderType, priority (Integer), requestedCompletionDate, requestedStartDate, startDate, state (ResourceOrderState)
  - Relationships: externalReference (list), note (list), orderItem (list of ResourceOrderItem), relatedParty (list)
  - getEntityState() returns state
- **ResourceOrderItem.java** — Embedded POJO (@Data)
  - Fields: tmfId, action (OrderItemAction), quantity (Integer), state (ResourceOrderItemState), appointment (AppointmentRef), orderItemRelationship (list), resource (ResourceRefOrValue embedded), resourceSpecification (ResourceSpecificationRef)
- **CancelResourceOrder.java** — Separate aggregate, extends EntityWithId, `@MappingEnabled(entityType = "cancel-resource-order")`
  - Properties: href, cancellationReason, effectiveCancellationDate, requestedCancellationDate, state (TaskState)
  - Relationship: resourceOrder (ResourceOrderRef)
- **ResourceOrderRef.java** — extends RefEntity, validates against ResourceOrder.TYPE_RESOURCE_ORDER
- **ResourceOrderState.java** — Enum: ACKNOWLEDGED, REJECTED, PENDING, HELD, IN_PROGRESS, CANCELLED, COMPLETED, FAILED, PARTIAL, ASSESSING_CANCELLATION, PENDING_CANCELLATION
- **ResourceOrderItemState.java** — Enum (same values as ResourceOrderState)
- **OrderItemAction.java** — Reuse from product-ordering or define: ADD, MODIFY, DELETE, NO_CHANGE
- **TaskState.java** — Reuse from product-ordering or define: ACCEPTED, TERMINATED_WITH_ERROR, IN_PROGRESS, DONE

#### REST controllers (`src/main/java/org/fiware/tmforum/resourceordering/rest/`)
- **ResourceOrderApiController.java** — extends AbstractApiController\<ResourceOrder\>, implements ResourceOrderApi
  - CRUD: create, list, retrieve, patch, delete
  - Validation: relatedParty references
- **CancelResourceOrderApiController.java** — extends AbstractApiController\<CancelResourceOrder\>, implements CancelResourceOrderApi
  - Create, list, retrieve
  - Validates resourceOrder reference exists
- **EventSubscriptionApiController.java** — extends AbstractSubscriptionApiController, implements EventsSubscriptionApi
  - Event groups: RESOURCE_ORDER, CANCEL_RESOURCE_ORDER

#### Mapper & Events
- **TMForumMapper.java** — extends BaseMapper, MapStruct mapper
  - Maps: ResourceOrder ↔ ResourceOrderVO, CancelResourceOrder ↔ CancelResourceOrderVO, enums, nested items
- **ResourceOrderingEventMapper.java** — implements ModuleEventMapper
  - Maps ResourceOrder and CancelResourceOrder to their VOs

#### Application & Config
- **Application.java** — @Factory, Micronaut.run()
- `src/main/resources/application.yaml` — port 8632, NGSI-LD config, basepath config
- `src/main/resources/application-in-memory.yaml`, `application-orion-ld.yaml`, `application-redis.yaml`, `application-scorpio.yaml` — copy from product-ordering-management

#### Integration tests (`src/test/java/org/fiware/tmforum/resourceordering/`)
- **ResourceOrderApiIT.java** — extends AbstractApiIT, implements ResourceOrderApiTestSpec
  - Parameterized tests for create (201/400), retrieve (200/404), list (200), patch (200/400), delete (204)
- **CancelResourceOrderApiIT.java** — extends AbstractApiIT, implements CancelResourceOrderApiTestSpec
- **EventSubscriptionApiIT.java** — extends AbstractSubscriptionApiController tests

### Files to modify
- `pom.xml` (parent) — add `<module>resource-order-management</module>`
- `all-in-one/pom.xml` — add source directory + OpenAPI generation execution

---

## Module 2: Service Order Management (TMF641)

**Module directory:** `service-order-management/`
**Package:** `org.fiware.tmforum.serviceordering`
**Generated packages:** `org.fiware.serviceordering.api` / `org.fiware.serviceordering.model`
**API base path:** `/tmf-api/serviceOrdering/v4`
**Spec:** `api/tm-forum/service-order-management/api.json`
**Reference module:** `product-ordering-management/` (closest analog)

### Files to create

#### Build config
- `service-order-management/pom.xml`
  - Dependencies: common, service-shared-models, resource-shared-models
  - OpenAPI generator pointing to `api/tm-forum/service-order-management/api.json`

#### Domain classes (`src/main/java/org/fiware/tmforum/serviceordering/domain/`)
- **ServiceOrder.java** — Main aggregate, `@MappingEnabled(entityType = "service-order")`
  - Properties: href, cancellationDate, cancellationReason, category, completionDate, description, expectedCompletionDate, externalId, notificationContact, orderDate, priority, requestedCompletionDate, requestedStartDate, startDate, state (ServiceOrderState)
  - Relationships: errorMessage (list), externalReference (list), jeopardyAlert (list), milestone (list), note (list), orderRelationship (list), serviceOrderItem (list), relatedParty (list)
- **ServiceOrderItem.java** — Embedded POJO
  - Fields: tmfId, action, quantity, state, appointment, errorMessage (list), service (ServiceRefOrValue), serviceOrderItem (nested list), serviceOrderItemRelationship (list)
- **CancelServiceOrder.java** — `@MappingEnabled(entityType = "cancel-service-order")`
  - Properties: href, cancellationReason, effectiveCancellationDate, requestedCancellationDate, state (TaskState)
  - Relationship: serviceOrder (ServiceOrderRef)
- **ServiceOrderRef.java** — extends RefEntity
- **ServiceOrderState.java** — Enum: ACKNOWLEDGED, REJECTED, PENDING, HELD, IN_PROGRESS, CANCELLED, COMPLETED, FAILED, PARTIAL, ASSESSING_CANCELLATION, PENDING_CANCELLATION
- **ServiceOrderItemState.java** — Enum (same values)
- **ServiceOrderRelationship.java** — POJO for order relationships
- **ServiceOrderErrorMessage.java** — POJO for error messages
- **ServiceOrderItemErrorMessage.java** — POJO for item-level errors
- **ServiceOrderJeopardyAlert.java** — POJO for jeopardy alerts
- **ServiceOrderMilestone.java** — POJO for milestones
- Reuse OrderItemAction and TaskState enums (or define locally)

#### REST controllers (`src/main/java/org/fiware/tmforum/serviceordering/rest/`)
- **ServiceOrderApiController.java** — CRUD for ServiceOrder
- **CancelServiceOrderApiController.java** — Create/list/retrieve for CancelServiceOrder
- **EventSubscriptionApiController.java** — Event groups: SERVICE_ORDER, CANCEL_SERVICE_ORDER

#### Mapper & Events
- **TMForumMapper.java** — Maps ServiceOrder, CancelServiceOrder, nested items, enums
- **ServiceOrderingEventMapper.java** — Maps ServiceOrder and CancelServiceOrder events

#### Application & Config
- **Application.java**, application.yaml (and profiles) — same pattern as Module 1

#### Integration tests
- **ServiceOrderApiIT.java**, **CancelServiceOrderApiIT.java**, **EventSubscriptionApiIT.java**

### Files to modify
- `pom.xml` (parent) — add `<module>service-order-management</module>`
- `all-in-one/pom.xml` — add source directory + OpenAPI generation execution

---

## Module 3: Software Management (TMF730)

**Module directory:** `software-management/`
**Package:** `org.fiware.tmforum.softwaremanagement`
**Generated packages:** `org.fiware.softwaremanagement.api` / `org.fiware.softwaremanagement.model`
**API base path:** `/tmf-api/softwareCompute/v4/`
**Spec:** `api/tm-forum/software-management/api.json`
**Reference module:** `resource-catalog/` and `resource-inventory/` (closest analogs — this API manages resources and resource specifications, not orders)

### Key difference

TMF730 does **not** follow the ordering pattern. It defines subtypes of Resource and ResourceSpecification for software/compute entities. The API exposes two top-level CRUD endpoints:
- `/resource` — CRUD for Resource (subtypes: InstalledSoftware, SoftwareResource, ResourceFunction, API, etc.)
- `/resourceSpecification` — CRUD for ResourceSpecification (subtypes: SoftwareSpecification, ResourceFunctionSpecification, APISpecification, etc.)

### Files to create

#### Build config
- `software-management/pom.xml`
  - Dependencies: common, resource-shared-models
  - OpenAPI generator pointing to `api/tm-forum/software-management/api.json`

#### Domain classes (`src/main/java/org/fiware/tmforum/softwaremanagement/domain/`)
- **SoftwareResource.java** — extends EntityWithId, `@MappingEnabled(entityType = "software-resource")`
  - Inherits Resource-like properties: href, name, description, category, operationalState, administrativeState, resourceStatus, usageState, startOperatingDate, endOperatingDate, resourceVersion
  - Relationships: activationFeature, attachment, note, place, relatedParty, resourceCharacteristic, resourceRelationship, resourceSpecification
- **SoftwareResourceSpecification.java** — extends EntityWithId, `@MappingEnabled(entityType = "software-resource-specification")`
  - Properties from ResourceSpecification: href, name, description, category, version, lifecycleStatus, isBundle, lastUpdate, validFor
  - Relationships: attachment, featureSpecification, relatedParty, resourceSpecCharacteristic, resourceSpecRelationship, targetResourceSchema

Since the OpenAPI spec uses the generic `Resource` and `ResourceSpecification` endpoints (not subtype-specific endpoints), the controllers will handle the base types. The subtypes (InstalledSoftware, SoftwareSpecification, ResourceFunction, ResourceFunctionSpecification, API, APISpecification, HostingPlatformRequirement, HostingPlatformRequirementSpecification, SoftwareSupportPackage, SoftwareSupportPackageSpecification) are represented via @type discriminator at the API level and will be handled through the generated VO model hierarchy.

#### REST controllers (`src/main/java/org/fiware/tmforum/softwaremanagement/rest/`)
- **ResourceApiController.java** — extends AbstractApiController\<SoftwareResource\>, implements ResourceApi
  - CRUD: create, list, retrieve, patch, delete
- **ResourceSpecificationApiController.java** — extends AbstractApiController\<SoftwareResourceSpecification\>, implements ResourceSpecificationApi
  - CRUD: create, list, retrieve, patch, delete
- **EventSubscriptionApiController.java** — event groups: RESOURCE, RESOURCE_SPECIFICATION

#### Mapper & Events
- **TMForumMapper.java** — Maps SoftwareResource ↔ ResourceVO, SoftwareResourceSpecification ↔ ResourceSpecificationVO
- **SoftwareManagementEventMapper.java** — Maps both entity types

#### Application & Config
- **Application.java**, application.yaml (and profiles) — same pattern

#### Integration tests
- **ResourceApiIT.java**, **ResourceSpecificationApiIT.java**, **EventSubscriptionApiIT.java**

### Files to modify
- `pom.xml` (parent) — add `<module>software-management</module>`
- `all-in-one/pom.xml` — add source directory + OpenAPI generation execution

---

## Implementation Order

### Step 1: Resource Order Management (TMF652)
Closest to the existing product-ordering-management pattern. Simplest of the three — 2 main entities (ResourceOrder + CancelResourceOrder), well-understood ordering pattern.

### Step 2: Service Order Management (TMF641)
Same ordering pattern but slightly richer model (adds jeopardyAlert, milestone, errorMessage). Can reuse patterns established in Step 1.

### Step 3: Software Management (TMF730)
Different pattern (catalog/inventory-style rather than ordering). Most complex model with 12 entity subtypes. Implemented last as it requires a different approach.

---

## Cross-cutting changes (after all 3 modules)

1. **Parent pom.xml** — Add all three modules to `<modules>` section
2. **all-in-one/pom.xml** — Add source directories and OpenAPI generation executions for all three
3. **CLAUDE.md** — Update if needed to mention the new modules

---

## Verification

For each module:
1. `mvn clean install -pl <module> -DskipITs` — Verify build & unit tests pass
2. `mvn integration-test -pl <module>` — Run integration tests with K3S broker
3. `mvn clean install -pl common,<module> -DskipTests -DskipITs` — Verify clean build with common

Full build verification:
```bash
mvn clean install -DskipITs
mvn integration-test -pl resource-order-management,service-order-management,software-management
```

---

## Key files to use as templates

| Pattern | Template file |
|---------|--------------|
| pom.xml | `product-ordering-management/pom.xml` |
| Controller (Order) | `product-ordering-management/src/main/java/.../rest/ProductOrderingApiController.java` |
| Controller (Cancel) | `product-ordering-management/src/main/java/.../rest/CancelProductOrderApiController.java` |
| Controller (Events) | `product-ordering-management/src/main/java/.../rest/EventSubscriptionApiController.java` |
| Domain (Order) | `product-ordering-management/src/main/java/.../domain/ProductOrder.java` |
| Domain (Cancel) | `product-ordering-management/src/main/java/.../domain/CancelProductOrder.java` |
| Mapper | `product-ordering-management/src/main/java/.../TMForumMapper.java` |
| EventMapper | `product-ordering-management/src/main/java/.../ProductOrderingEventMapper.java` |
| Application | `product-ordering-management/src/main/java/.../Application.java` |
| application.yaml | `product-ordering-management/src/main/resources/application.yaml` |
| Integration test | `product-ordering-management/src/test/java/.../ProductOrderingApiIT.java` |
| Resource/Spec controllers (TMF730) | `resource-catalog/src/main/java/.../rest/ResourceCatalogApiController.java` |
