# Common

This module is intended to provide functionality, required by most of the API-implementations.

## Referential integrity

For the api-implementation, it's important to contain [referential-integrity](https://en.wikipedia.org/wiki/Referential_integrity).
To support such checks on a Pojo-level the [ReferenceValidationService](src/main/java/org/fiware/tmforum/common/validation/ReferenceValidationService.java) can be used.
Any entity that implements the [ReferencedEntity](src/main/java/org/fiware/tmforum/common/validation/ReferencedEntity.java) interface, can be handed over to the ReferenceValidationService 
and checked for references.