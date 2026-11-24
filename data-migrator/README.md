# Data-Migrator

Migration tool, to support migration of the data-model between TMForum Version 0.32.1 and the current 1.3.x.

## How it works

The migrator loads the entities, using the old version of
the [ngsi-ld-mapping library](https://github.com/wistefan/ngsi-ld-java-mapping), from ContextBroker A
and then creates them with the current version of the library at ContextBroker B. This allows a minimum downtime
migration, by disabling write access to Broker A(while keeping read access)
and switching over to the new broker once migration is finished.
In order to have both library-versions as part of one application the following structure is used:

* the legacy-loader uses the old version for loading data. The 1.2.11 library ships Micronaut 3 beans, so it is no longer
  part of this build and is built from the last Micronaut 3 release, set in `legacy.loader.ref` in [pom.xml](./pom.xml)
* module [update-writer](./update-writer) to use the new version for writing data
* module [migrator](./migrator) to load both libraries into different classloaders and transfer objects between them

## Build the legacy-loader

The container image builds it automatically. To build the jar locally, check out the release from `legacy.loader.ref` (1.18.3 below) and build it with
JDK 17:

```shell
    git fetch --no-tags https://github.com/FIWARE/tmforum-api tag 1.18.3
    git worktree add /tmp/legacy-loader 1.18.3
    mvn -f /tmp/legacy-loader/pom.xml -pl data-migrator/legacy-loader -am package -DskipTests -DskipITs
```

The jar is written to `/tmp/legacy-loader/data-migrator/legacy-loader/target/legacy-loader-0.1.jar`.

## Limitation: the migrated domain model is pinned to the legacy-loader release

The legacy-loader jar contains the domain classes of `legacy.loader.ref`, and the entities it hands to the
`update-writer` are instances of those classes. `JavaObjectMapper` reads their mapping annotations
(`@AttributeGetter`, `@AttributeSetter`, `@EntityType`, ...), so the migrated data carries the attribute names of
that release and not of the current one.

With `legacy.loader.ref` at `1.18.3` these attributes differ from the current domain model:

| Class                                 | Written as     | Current        |
|---------------------------------------|----------------|----------------|
| `RefEntity.atReferredType`            | `@referredType`| `atReferredType` |
| `RelatedProductOrderItemRef.atReferredType` | `@referredType` | `atReferredType` |
| `TMForumSubscription.query`/`rawQuery`| unescaped      | forbidden characters escaped |

Entities using them have to be fixed up in the target broker after the migration. The list grows with every later
change to a mapping annotation, so check it against the diff between `legacy.loader.ref` and the release you migrate
to before running a migration.

## Run

The migration tool can either be run directly as a jar:

```shell
    java -jar ./migrator/target/migrator-0.1.jar  --help
    java --add-opens java.base/java.lang=ALL-UNNAMED -jar ./migrator/target/migrator-0.1.jar -rB http://localhost:8080 -wB http://localhost:1026 -ll /tmp/legacy-loader/data-migrator/legacy-loader/target/legacy-loader-0.1.jar -uw update-writer/target/update-writer-0.1.jar 
```

Or as a container:
```shell
    docker run --env READ_BROKER="http://my-read-broker.org" --env WRITE_BROKER="http://my-write-broker.org"  quay.io/fiware/tmforum-migrator
```
## postgresql-migrator

[postgresql-migrator](./postgresql-migrator) moves a broker-backed deployment (Orion-LD or Scorpio) onto the PostgreSQL
backend, and back. It also backfills inline references for deployments that stay on the broker. It reads and writes
through the application's own domain model and storage adapters (it depends on `all-in-one`), so the stored documents
come out exactly as the application writes them. The step-by-step procedure is in
[RUNBOOK_POSTGRESQL.md](./RUNBOOK_POSTGRESQL.md).

### Commands

| Command                 | What it does |
|-------------------------|--------------|
| `import`                | Copies every entity from the broker into `tmforum.entity`, keeping `createdAt`/`modifiedAt` and firing no TMForum event. Seeds `version` `0.0.1` on versioned types that have none. `--backfill-references` runs the PostgreSQL backfill right after a successful import. |
| `backfill-references`   | `--target postgresql`: one SQL transaction that fills the absent fields (`name`, `href`, ...) of stored references from their local targets. `--target broker`: the same on the broker, for deployments that stay on NGSI-LD. Only the 81 relationships that were resolved on read before inline references are touched; a field the reference already carries is never replaced. |
| `verify`                | Compares broker and PostgreSQL: counts per type, every top-level attribute, a field-level diff (all entities of small types, a sample otherwise) and the escaping of hub queries. `--direction export` checks an `export` instead of an `import`. |
| `replay`                | Sends captured API requests to the broker-backed and the PostgreSQL-backed deployment and diffs the responses. |
| `export`                | The rollback: copies PostgreSQL back into an empty broker and registers a new NGSI-LD subscription per hub, notifying `--notification-endpoint`. |
| `cleanup-subscriptions` | After the rollback window: deletes the broker subscriptions of the hubs and drops the hubs' `subscription` attribute in PostgreSQL. Needs `--confirm-cutover-final`. |

Every command takes `--dry-run`, prints a report per type and exits non-zero on any failure. `--help` on a command
lists all of its options.

### Common options

| Option                                   | Default                 | Description |
|------------------------------------------|-------------------------|-------------|
| `--broker-url`                           | `http://localhost:1026` | NGSI-LD broker of the broker-backed deployment. |
| `--broker-type`                          | `orion-ld`              | `orion-ld` or `scorpio`. |
| `--tenant`                               | none                    | NGSI-LD tenant, as `general.tenant` of the deployment. |
| `--source-escaped`                       | `true`                  | `general.escape-forbidden-characters` of the broker-backed deployment. |
| `--replace-on-update`                    | none                    | `general.replace-on-update` of the deployment; required by `backfill-references --target broker` on Scorpio. |
| `--pg-host`, `--pg-port`, `--pg-db`      | `localhost`, `5432`, `tmforum` | PostgreSQL; env `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`. |
| `--pg-user`, `--pg-password`             | `tmforum`               | env `POSTGRES_USER`, `POSTGRES_PASSWORD`. |
| `--types`                                | all mapped types        | `import`, `verify`, `export`: the entity types to handle. |
| `--page-size`                            | `100`                   | Entities per broker request; Orion-LD caps it at 1000. |
| `--resume`                               | off                     | `import`/`export`: continue an interrupted run, skipping ids that are already stored. |
| `--fail-fast`                            | off                     | `import`/`export`: stop at the first failure. |

`micronaut.environments` and `MICRONAUT_ENVIRONMENTS` must not be set: the migrator sets the environments of its
broker and PostgreSQL contexts itself.

### Build and run

```shell
    mvn install -pl data-migrator/postgresql-migrator -am -DskipTests -DskipITs
    java -jar data-migrator/postgresql-migrator/target/postgresql-migrator-0.1.jar import --help
```

The image is built with `mvn install -Poci -pl data-migrator/postgresql-migrator -am -DskipTests -DskipITs`; the
arguments after the image name are the command:

```shell
    docker run --env POSTGRES_HOST=postgres.example.org --env POSTGRES_PASSWORD=secret \
        quay.io/fiware/tmforum-postgresql-migrator import --broker-url http://orion-ld:1026 --dry-run
```

### Tests

`mvn test -pl data-migrator/postgresql-migrator` runs the unit tests. The ITs need a broker, so run them with
`-Dbroker=orion-ld` or `-Dbroker=scorpio` (and `-Dcache=in-memory|redis`). `MigrationRehearsalIT` rehearses the
whole runbook with two in-process `all-in-one` servers: import, backfill, verify, replay of
[requests.jsonl](./postgresql-migrator/src/test/resources/replay/requests.jsonl), writes on PostgreSQL, `export`,
then replay again. It also checks that every restored hub owns its NGSI-LD subscription to `--notification-endpoint`
(the broker path's DeleteEvent channel), and that both hubs get a create notification from the restored broker
deployment. That deployment sends the notification in-process from the restored hub entities.

### `replay` capture format

`replay` reads a JSON-lines file, one captured API request per line:

```json
{"method": "GET", "path": "/tmf-api/productCatalogManagement/v4/productOffering?limit=10", "headers": {"Accept": "application/json"}}
{"method": "PATCH", "path": "/tmf-api/productCatalogManagement/v4/productOffering/urn:ngsi-ld:product-offering:1", "headers": {"Content-Type": "application/merge-patch+json"}, "body": {"name": "renamed"}}
```

- `method` and `path` are required; `path` starts with `/` and keeps its query string.
- `headers` is optional and sent as captured, except `Host`, `Content-Length`, `Connection`, `Expect`, `Upgrade`,
  `Transfer-Encoding` (set by the HTTP client) and `Accept-Encoding` (dropped so both deployments answer uncompressed
  and the bodies can be compared as JSON).
- `body` is optional: a JSON string is sent as its text, any other JSON value as JSON.

Each request goes to `--broker-base-url` and `--postgres-base-url`, and the status and JSON bodies are diffed. The diff
ignores object key order, the server host of every absolute `href`, every field but `id` and `href` of a reference to
an `--external-host`, and the JSON pointers in `--ignore` (`*` matches one key or index, `**` any depth; a trailing
`**` drops everything below its parent, which stays as an empty list or object). Lists are compared as sets only for
paths matched by `--unordered-paths`. `POST`, `PUT`, `PATCH` and `DELETE` are skipped unless `--include-writes` is
set, which is meant for rehearsal environments only. The per-request diffs go to `--report`
(default `replay-report.json`), and the command exits non-zero on any difference or failed request.
