# Release Notes: caching of the schemas linked via @schemaLocation

## Overview

Entities that carry an `@schemaLocation` are validated against the JSON Schema found at that address. Until now,
that schema was downloaded again for **every single request** touching such an entity: the deserializer created a
new `SchemaRegistry` per call, so the registry-internal cache never had a chance to be used, and the schema
provider was contacted inside the (blocking) deserialization of each request.

Downloaded schemas are now kept in an in-memory cache with a time-to-live.

## What changed

* The `SchemaRegistry` used for validation is created once, as a singleton bean
  (`SchemaRegistryProvider`), instead of per deserialization.
* Its resource loader for remote schemas is wrapped into a `CachingResourceLoader`, keeping the downloaded
  documents in a cache that is bound in time (time-to-live) and in size (least recently used entries are
  evicted). Schemas referenced from a schema via `$ref` are cached the same way.
* Failed downloads are not cached, e.g. a temporarily unavailable schema provider is retried with the next
  request.
* The registry-internal schema cache is explicitly disabled. It never expires its entries, so it would keep the
  parsed schemas forever and the configured time-to-live would never take effect.

## Configuration

| Property                        | Default | Description                                                                                            |
|---------------------------------|---------|---------------------------------------------------------------------------------------------------------|
| `general.schema-cache.enabled`     | `true`  | Enables the caching of downloaded schemas. If disabled, every validation downloads the schema again.      |
| `general.schema-cache.time-to-live`| `30m`   | Time a downloaded schema is kept, before it is downloaded again. A duration of `0s` disables caching.     |
| `general.schema-cache.max-size`    | `100`   | Maximum number of schemas to be kept in the cache.                                                        |

```yaml
general:
  schema-cache:
    enabled: true
    time-to-live: 30m
    max-size: 100
```

The cache is bounded on purpose: the schema addresses are provided by the clients, an unbounded cache would allow
them to fill up the memory through arbitrary addresses.

## Impact

* Validation against `@schemaLocation` no longer performs an http-request per request, as long as the schema is
  cached.
* A schema that changes at its provider is picked up after at most the configured time-to-live. Set
  `general.schema-cache.enabled: false` to get the previous behaviour of always downloading the schema.

## Migration

None. The caching is enabled by default and no api-behaviour changes.
