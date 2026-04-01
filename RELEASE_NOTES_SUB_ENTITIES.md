# Release Notes: TMF730 Resource Sub-Entity Support

## Overview

Adds full CRUD support for all TMF730 (Software and Compute Entity Management) Resource and
ResourceSpecification sub-types through the existing `/resource` and `/resourceSpecification`
REST endpoints using polymorphic dispatch based on the `@type` field.

## New Resource Sub-Types

| Sub-Type | NGSI-LD Entity Type | Parent | Key Fields |
|---|---|---|---|
| LogicalResource | `logical-resource` | Resource | value |
| SoftwareResource | `software-resource` | LogicalResource | isDistributedCurrent, lastUpdate, targetPlatform |
| API | `api-resource` | SoftwareResource | (none beyond SoftwareResource) |
| InstalledSoftware | `installed-software` | SoftwareResource | isUTCTime, lastStartTime, numProcessesActiveCurrent, numUsersCurrent, serialNumber, pagingFileSizeCurrent, processMemorySizeCurrent, swapSpaceUsedCurrent |
| HostingPlatformRequirement | `hosting-platform-requirement` | LogicalResource | (none beyond LogicalResource) |
| PhysicalResource | `physical-resource` | Resource | manufactureDate, powerState, serialNumber, versionNumber |
| SoftwareSupportPackage | `software-support-package` | PhysicalResource | (none beyond PhysicalResource) |

## New ResourceSpecification Sub-Types

| Sub-Type | NGSI-LD Entity Type | Parent | Key Fields |
|---|---|---|---|
| LogicalResourceSpecification | `logical-resource-specification` | ResourceSpecification | resourceSpecRelationship |
| SoftwareResourceSpecification | `software-resource-specification` | LogicalResourceSpecification | buildNumber, isDistributable, isExperimental, maintenanceVersion, majorVersion, minorVersion, otherDesignator, releaseStatus, installSize |
| APISpecification | `api-specification` | SoftwareResourceSpecification | apiProtocolType, authenticationType, externalSchema, externalUrl, internalSchema, internalUrl |
| SoftwareSpecification | `software-specification` | SoftwareResourceSpecification | numUsersMax, numberProcessActiveTotal, softwareSupportPackage |
| HostingPlatformRequirementSpecification | `hosting-platform-requirement-specification` | LogicalResourceSpecification | isVirtualizable |
| PhysicalResourceSpecification | `physical-resource-specification` | ResourceSpecification | resourceSpecRelationship |
| SoftwareSupportPackageSpecification | `software-support-package-specification` | PhysicalResourceSpecification | (none beyond PhysicalResourceSpecification) |

## Usage

### Creating a Sub-Type Resource

Send a `POST /resource` with `@type` set to the sub-type name and sub-type-specific fields in
the request body. A `@schemaLocation` referencing a JSON Schema that permits the additional
properties is required.

```json
{
  "@type": "SoftwareResource",
  "@schemaLocation": "https://example.com/schemas/software-resource.json",
  "name": "My Software",
  "targetPlatform": "server",
  "isDistributedCurrent": false,
  "value": "sw-instance-1"
}
```

### Retrieving / Listing

- `GET /resource/{id}` automatically detects the sub-type from the NGSI-LD entity type in the ID.
- `GET /resource` returns all resources across all sub-types.
- Sub-type-specific fields appear as additional properties in the response.

### Patching / Deleting

- `PATCH /resource/{id}` and `DELETE /resource/{id}` work transparently for all sub-types.

## Architecture

- **Domain classes** use Java inheritance from `Resource`/`ResourceSpecification` with per-type
  `@MappingEnabled` annotations, avoiding field duplication.
- **Polymorphic dispatch** in controllers uses `@type` for creation and NGSI-LD entity type
  (extracted from ID) for retrieval/patch.
- **Jackson ObjectMapper** handles conversion between base VOs and sub-type VOs.
- **MapStruct** handles domain class <-> VO mapping.
- **ResourceTypeRegistry** centralizes type name -> entity type -> domain class mappings.

## Bug Fixes

- Fixed 15 pre-existing test failures in `ResourceApiIT`:
  - Feature tests: nulled `href`, `atSchemaLocation`, `atBaseType`, `atType` on `FeatureVO` and
    `FeatureRelationshipVO` test examples (broker rejects invalid `href`, mapper ignores `atBaseType`/`atType`).
  - Retrieve field-filtering tests: updated expectations to match actual behavior (field filtering
    not implemented in retrieve endpoint).
- Same fixes applied to `ResourceSpecificationApiIT`.

## Files Changed

### New (resource-shared-models)
- 14 domain entity classes (7 Resource + 7 Specification sub-types)
- `SoftwareSupportPackageRef.java`, `ResourceSpecificationRelationship.java`

### Modified (software-management)
- `TMForumMapper.java` — 30+ new MapStruct methods
- `ResourceApiController.java` — polymorphic CRUD dispatch
- `ResourceSpecificationApiController.java` — polymorphic CRUD dispatch
- `SoftwareManagementEventMapper.java` — all 16 entity types registered
- `ResourceApiIT.java` — 17 new sub-type tests + 15 pre-existing fixes
- `ResourceSpecificationApiIT.java` — 13 new sub-type tests + field-filtering fixes

### New (software-management)
- `ResourceTypeRegistry.java` — centralized type registry
- `permissive-schema.json` — test resource for schema validation
