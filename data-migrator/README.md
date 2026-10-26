# Data-Migrator

Migration tool, to support migration of the data-model between TMForum Version 0.32.1 and the current 1.3.x.

## How it works

The migrator loads the entities, using the old version of
the [ngsi-ld-mapping library](https://github.com/wistefan/ngsi-ld-java-mapping), from ContextBroker A
and then creates them with the current version of the library at ContextBroker B. This allows a minimum downtime
migration, by disabling write access to Broker A(while keeping read access)
and switching over to the new broker once migration is finished.
In order to have both library-versions as part of one application the following structure is used:

* the legacy-loader uses the old version for loading data. It needs Micronaut 4, so it is no longer part of this build
  and is built from the Micronaut 4 release set in `legacy.loader.ref` in [pom.xml](./pom.xml)
* module [update-writer](./update-writer) to use the new version for writing data
* module [migrator](./migrator) to load both libraries into different classloaders and transfer objects between them

## Build the legacy-loader

The container image builds it automatically. To build the jar locally, check out the release from `legacy.loader.ref` (1.22.0 below) and build it with
JDK 25:

```shell
    git fetch --no-tags https://github.com/FIWARE/tmforum-api tag 1.22.0
    git worktree add /tmp/legacy-loader 1.22.0
    mvn -f /tmp/legacy-loader/pom.xml -pl data-migrator/legacy-loader -am package -DskipTests -DskipITs
```

The jar is written to `/tmp/legacy-loader/data-migrator/legacy-loader/target/legacy-loader-0.1.jar`.

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