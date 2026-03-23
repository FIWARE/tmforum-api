# Release Notes: Add New TMForum API Modules

## Overview

Three new TMForum API modules have been added to the project, extending the FIWARE TMForum API implementation with resource ordering, service ordering, and software management capabilities.

## New Modules

### Resource Order Management (TMF652)

- **Module:** `resource-order-management/`
- **Package:** `org.fiware.tmforum.resourceordering`
- **API base path:** `/tmf-api/resourceOrderingManagement/v4/`
- **Entities:** ResourceOrder, CancelResourceOrder
- **Description:** Full CRUD support for resource ordering, following the same ordering pattern as product-ordering-management. Includes resource order lifecycle management with states (ACKNOWLEDGED, PENDING, IN_PROGRESS, COMPLETED, etc.) and cancel order support with task state tracking.

### Service Order Management (TMF641)

- **Module:** `service-order-management/`
- **Package:** `org.fiware.tmforum.serviceordering`
- **API base path:** `/tmf-api/serviceOrdering/v4/`
- **Entities:** ServiceOrder, CancelServiceOrder
- **Description:** Full CRUD support for service ordering. Extends the ordering pattern with richer model elements including jeopardy alerts, milestones, error messages, and order relationships. Supports nested service order items with service references.

### Software Management (TMF730)

- **Module:** `software-management/`
- **Package:** `org.fiware.tmforum.softwaremanagement`
- **API base path:** `/tmf-api/softwareCompute/v4/`
- **Entities:** Resource (software), ResourceSpecification (software)
- **Description:** Catalog/inventory-style API for managing software and compute resources and their specifications. Reuses Resource and ResourceSpecification domain models from resource-shared-models. Supports multiple software subtypes (InstalledSoftware, SoftwareResource, ResourceFunction, API, etc.) via type discriminator.

## Cross-Cutting Changes

- **Parent `pom.xml`:** All three modules added to the `<modules>` section.
- **`all-in-one/pom.xml`:** Source directories and OpenAPI generation executions added for all three modules, enabling the single uber-JAR deployment to include these APIs.

## Integration Tests

Each module includes comprehensive integration tests covering:
- Entity creation (201 Created, 400 Bad Request for invalid input)
- Entity retrieval (200 OK, 404 Not Found)
- Entity listing (200 OK)
- Entity update/patch (200 OK, 400 Bad Request)
- Entity deletion (204 No Content) where applicable
- Event subscription management (201, 204, 400)
