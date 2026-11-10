# storage-postgresql

PostgreSQL (18) adapter of the `storage-api` port. Its beans are active only in the Micronaut environment
`postgresql` (`PostgresStorage.ENVIRONMENT`), so the NGSI-LD adapter in `common` stays the default. The data path
uses R2DBC. Flyway migrates the schema over JDBC at startup, and nothing else uses that JDBC connection.

## Schema

Flyway owns the schema `tmforum`: it creates it and keeps its history table (`tmforum.flyway_schema_history`)
there. The migrations are in `src/main/resources/db/migration/tmforum`.

`tmforum.entity` holds one row per entity. `data` is the domain object as JSON. `version`, `revision` and
`is_current` are reserved for versioning and keep their defaults (`''`, `1`, `true`) for now: `version` is `''` for
unversioned types, `revision` is monotonic per `id`. The partial unique index
`entity_current_idx` allows only one current row per `id`.

| Column       | Type          | Default |
|--------------|---------------|---------|
| `id`         | `text`        |         |
| `version`    | `text`        | `''`    |
| `revision`   | `bigint`      | `1`     |
| `is_current` | `boolean`     | `true`  |
| `type`       | `text`        |         |
| `data`       | `jsonb`       |         |
| `created_at` | `timestamptz` | `now()` |
| `updated_at` | `timestamptz` | `now()` |

All columns are `NOT NULL`. The primary key is `(id, version)`.

## Configuration

Put this block in the `application-postgresql.yaml` of the API module. Both datasources must point at the same
database. The `r2dbc:pool:` URL puts an `r2dbc-pool` connection pool in front of the driver. Without it, every
statement opens its own connection.

micronaut-r2dbc turns the kebab-case `options` keys into r2dbc-pool's camelCase options (`initial-size` becomes
`initialSize`). r2dbc-pool reads durations with `Duration.parse`, so they must be ISO-8601 (`PT10S`, not `10s`).
r2dbc-pool has no acquire or create timeout by default, so both are bounded here: when every connection is busy or
the database cannot be reached, a request fails after `max-acquire-time` with UNAVAILABLE (503) instead of hanging:

```yaml
r2dbc:
  datasources:
    default:
      url: r2dbc:pool:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5432}/${POSTGRES_DB:tmforum}
      username: ${POSTGRES_USER:tmforum}
      password: ${POSTGRES_PASSWORD:tmforum}
      options:
        initial-size: ${POSTGRES_POOL_INITIAL_SIZE:2}
        max-size: ${POSTGRES_POOL_MAX_SIZE:10}
        max-acquire-time: ${POSTGRES_POOL_MAX_ACQUIRE_TIME:PT10S}
        max-create-connection-time: ${POSTGRES_POOL_MAX_CREATE_CONNECTION_TIME:PT10S}

flyway:
  datasources:
    default:
      enabled: true
      url: jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5432}/${POSTGRES_DB:tmforum}
      user: ${POSTGRES_USER:tmforum}
      password: ${POSTGRES_PASSWORD:tmforum}
      schemas: tmforum
      create-schemas: true
      locations: classpath:db/migration/tmforum
```

| Environment variable                       | Default     |
|--------------------------------------------|-------------|
| `POSTGRES_HOST`                            | `localhost` |
| `POSTGRES_PORT`                            | `5432`      |
| `POSTGRES_DB`                              | `tmforum`   |
| `POSTGRES_USER`                            | `tmforum`   |
| `POSTGRES_PASSWORD`                        | `tmforum`   |
| `POSTGRES_POOL_INITIAL_SIZE`               | `2`         |
| `POSTGRES_POOL_MAX_SIZE`                   | `10`        |
| `POSTGRES_POOL_MAX_ACQUIRE_TIME`           | `PT10S`     |
| `POSTGRES_POOL_MAX_CREATE_CONNECTION_TIME` | `PT10S`     |

## Tests

Every IT run provisions PostgreSQL in the k3s test cluster (`k3s/postgresql-db`, port 5432, database, user and
password `tmforum`), next to the broker. `-Dbroker=postgresql` starts no broker, only Redis (`k3s/postgresql`).

```bash
mvn verify -pl storage-postgresql                       # SchemaMigrationIT
```
