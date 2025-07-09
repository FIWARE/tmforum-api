# Data-Migrator

Migration tool, to support migration of the data-model between TMForum Version 0.32.1 and the current 1.3.x.

## How it works

The migrator loads the entities, using the old version of
the [ngsi-ld-mapping library](https://github.com/wistefan/ngsi-ld-java-mapping), from ContextBroker A
and then creates them with the current version of the library at ContextBroker B. This allows a minimum downtime
migration, by disabling write access to Broker A(while keeping read access)
and switching over to the new broker once migration is finished.
In order to have both library-versions as part of one application the following structure is used:

* module [legacy-loader](./legacy-loader) to use the old version for loading data
* module [update-writer](./update-writer) to use the new version for writing data
* module [migrator](./migrator) to load both libraries into different classloaders and transfer objects between them

## Run

```shell
    java -jar ./migrator/target/migrator-0.1.jar  --help
    java --add-opens java.base/java.lang=ALL-UNNAMED -jar ./migrator/target/migrator-0.1.jar -rB http://localhost:8080 -wB http://localhost:1026 -ll ./legacy-loader/target/legacy-loader-0.1.jar -uw update-writer/target/update-writer-0.1.jar 
```