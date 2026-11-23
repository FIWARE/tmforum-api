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
## postgresql-migrator: `replay` capture format

`replay` reads a JSON-lines file, one captured API request per line:

```json
{"method": "GET", "path": "/tmf-api/productCatalogManagement/v4/productOffering?limit=10", "headers": {"Accept": "application/json"}}
{"method": "PATCH", "path": "/tmf-api/productCatalogManagement/v4/productOffering/urn:ngsi-ld:product-offering:1", "headers": {"Content-Type": "application/merge-patch+json"}, "body": {"name": "renamed"}}
```

- `method` and `path` are required; `path` starts with `/` and keeps its query string.
- `headers` is optional and sent as captured.
- `body` is optional: a JSON string is sent as its text, any other JSON value as JSON.

Each request goes to `--broker-base-url` and `--postgres-base-url`, and the status and JSON bodies are diffed. The diff
ignores object key order, the server host of every absolute `href`, every field but `id` and `href` of a reference to
an `--external-host`, and the JSON pointers in `--ignore` (`*` matches one key or index, `**` any depth). Lists are
compared as sets only for paths matched by `--unordered-paths`. `POST`, `PUT`, `PATCH` and `DELETE` are skipped unless
`--include-writes` is set, which is meant for rehearsal environments only. The per-request diffs go to `--report`
(default `replay-report.json`), and the command exits non-zero on any difference or failed request.
