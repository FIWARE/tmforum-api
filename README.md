# FIWARE implementation of the TMFORUM-APIs

## Structure

The project is setup as a maven multi-module project, to ease the development of independent implementations for each
API.

Functionality required by most of the implementations is contained in the [common-module](common). Beside that, the
project relies on the [ngsi-ld-java-mapping](https://github.com/wistefan/ngsi-ld-java-mapping) library, to provide an
annotation based mapping between NGSI-LD object and Java-Pojos to support the usage of a decoupling layer.

The api-implementations are inside modules, producing jar-files and oci-containers according to the definitions in
the [parent-pom](pom.xml). Since some models are used in multiple apis, there are modules for ```shared-models```. They
should only include models that are used by other modules, too.

The [ui-module](ui) is a read-only browser for the APIs of the all-in-one deployment: it lists the entities and
follows the references between them as hyperlinks. It is an image of its own (`tmforum-ui`), not part of the
all-in-one: run it next to it as a sidecar, or on its own to debug. See [ui/README.md](ui/README.md).

The project also contains 3 non-module folders:

- [api](api) - contains the [OpenApi-Specifications](https://spec.openapis.org/oas/v3.1.0) of the NGSI-LD API used by
  the project. It does not contain the TMForum-API specs, since they are downloaded on built-time.
- [k3s](k3s) - contains kubernetes manifests to be used in the integration-test environments or for setting up a
  dev-environment. Currently includes:
    - [Orion-LD Context Broker](https://github.com/FIWARE/context.Orion-LD)
    - [MongoDB](https://www.mongodb.com/)
- [conformance-test](conformance-test) - contains a dockerfile to be used for running the TMForum-Conformance Tests
  inside the K3S setup. Also contains a kubernetes manifest for running the api-implementations.

## Building

The project is built with [Micronaut 5](https://micronaut.io/) and needs JDK 25 (Jackson 3 is used for JSON). Build
all modules with ```mvn clean install -DskipTests -DskipITs -Dk3s.skip=true```. ```-DskipITs``` does not stop the k3s
cluster for the integration-tests from starting, ```-Dk3s.skip=true``` does. The ```oci``` profile builds the container
images in the ```deploy``` phase and pushes them (amd64 and arm64) to ```${image.registry}/${image.repository}```:
```mvn deploy -Poci -DskipTests -DskipITs -Dk3s.skip=true -Dimage.registry=<REGISTRY> -Dimage.tag=<TAG>```. The images ship
their own Java 25 runtime.

## Testing

The current implementation supports multiple layers of testing:

- Unit-Tests, using [JUnit5](https://junit.org/junit5/docs/current/user-guide/) and [Mockito](https://site.mockito.org/)
  to test on a function-level
- Unit-Tests,
  using [JUnit5](https://junit.org/junit5/docs/current/user-guide/),[Micronaut-Test](https://micronaut-projects.github.io/micronaut-test/latest/guide/)
  and [Mockito](https://site.mockito.org/) to test on component level(e.g. running parts of the application, while
  mocking all external dependencies)
- Integration-Tests,
  using [JUnit5](https://junit.org/junit5/docs/current/user-guide/),[Micronaut-Test](https://micronaut-projects.github.io/micronaut-test/latest/guide/)
  and [k3s](https://k3s.io/) to test the application with its real external dependencies running
- Conformance-Test, using the CTK-Implementations provided by TMForum and [k3s](https://k3s.io/) to test conformance of
  the implementations

In order to reduce the overhead of test implementation, the OpenAPI-Generator provides the following test-helpers:

- Test-Interface: an interface, defining a test for all endpoint-response combinations possible from the OpenAPI-spec -
  see [OrganizationApiIT](party/src/test/java/org.fiware.tmforum.party/OrganizationApiIT.java) as an example
- Test-Examples: for each model generated(f.e. ```Organization```), a test example builder is generated, that provides
  pre-filled(from the spec-examples and default values) test-objects -
  see   [OrganizationApiIT](party/src/test/java/org.fiware.tmforum.party/OrganizationApiIT.java) as an example

### Running unit-tests

The unit-tests are integrated into
the [maven-lifecycle's](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)
```test-phase```
via the [surfire-plugin](https://maven.apache.org/surefire/maven-surefire-plugin/). They can be executed
via ```mvn test```. Since they don't have any external dependencies, they also can be run via the IDEs(only tested
with [IntelliJ](https://www.jetbrains.com/idea/)) integrated test runners. All tests with suffix ```*Test``` will be
executed.

### Running integration-tests

The integration-tests are integrated into
the [maven-lifecycle's](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)
```integration-test phase```
via the [failsafe-plugin](https://maven.apache.org/surefire/maven-failsafe-plugin/). Since the tests require a running
broker at ```localhost:1026```, the [k3s-plugin](https://github.com/kokuwaio/k3s-maven-plugin) is integrated in
the ```pre-integration-test``` and ```post-intgeration-test``` phases to spin-up a k3s-cluster inside docker and deploy
the components. The tests can be executed via: ```mvn integration-test```. After running the tests, the k3s-cluster will
be automatically destroyed. All tests with suffix ```*IT``` will be executed.

In order to run the tests from the IDE, you have to setup an environment before that. This can be achieved via
the ```dev```-[profile](https://maven.apache.org/guides/introduction/introduction-to-profiles.html).
With ```mvn install -Pdev```, a k3s-cluster will be setup but not destroyed after running the tests. In order to connect
to the cluster, install the [kubectl-client](https://kubernetes.io/docs/tasks/tools/#kubectl) and
run ```export KUBECONFIG=/tmp/k3s-maven-plugin/mount/kubeconfig.yaml && kubectl get all --all-namespaces```. To clean up
the environment, use ```mvn clean k3s:rm -Pdev```.

### Running conformance-tests

> **Note:** the CTK downloads referenced by `module.ctk.url` currently return 404 for every module, so the profile
> fails until TMForum publishes them again or the URLs are updated.

TMForum does provide conformance-test tooling for the
API-implementations: [Open API Conformance](https://projects.tmforum.org/wiki/display/API/Open+API+Conformance)
The tests are integrated into the maven lifecycle with the "conformance-test" profile. To execute them, run "mvn clean
install -Pconformance-test --projects common,resource-shared-models,customer-shared-models,service-shared-models,product-shared-models,<THE_MODULE> -DskipTests -DskipITs".
The tests are mounted into a
docker container and executed using them, after the API-implementation and Orion-LD are started. Currently, they need to
run individually for each API-Module, since the tests expect them under the same port on localhost. To integrate a new
module into the tests:

- set find the test url of the module(at
  the [TMForum OpenAPI table](https://projects.tmforum.org/wiki/display/API/Open+API+Table) in the column "CTK")
- insert it to the property <module.ctk.url> in your module
- check the structure of the zip-file, containing the test and set the following properties accordingly(example from the
  parties-api)

```xml

<module.ctk.script-folder>TMF632-Party</module.ctk.script-folder>
<module.ctk.run-script>Mac-Linux-RUNCTK.sh</module.ctk.run-script>
<module.ctk.base-path>/tmf-api/party/v4</module.ctk.base-path>
```

- add the module to the matrix input of the [conformance-test](.github/workflows/conformance-test.yaml) workflow

## Dev-Environment

To get a dev-environment, the [k3s-plugin](https://github.com/kokuwaio/k3s-maven-plugin) can be
used: ```mvn install -Pdev```
After running this, the context broker is available at ```http://localhost:1026```
See the logs via:

```
  export KUBECONFIG=$(pwd)/target/k3s.yaml && kubectl get all --all-namespaces
  kubectl logs <BROKER_POD>
```

## Broker compatibility

### Attribute updates (`replaceOnUpdate`)

A PATCH only writes the attributes it contains. Every other attribute of the entity stays as it is stored, so
concurrent PATCHes of different attributes do not interfere with each other. The property `general.replaceOnUpdate`
controls how the attributes of a PATCH are written:

- `false` (default): the attributes are appended as they are, with `POST /entities/{id}/attrs`.
- `true`: each attribute of the PATCH replaces the stored one as a whole:
  - a list ends up with exactly the items of the PATCH, items that are no longer part of it are removed
  - an empty list removes the attribute
  - an object value is replaced, not merged

  To do so, the API reads the stored entity and sends a single `PATCH /entities/{id}/attrs` with the new attribute
  instances and an NGSI-LD Null (`urn:ngsi-ld:null`) for every stored instance (identified by its `datasetId`) that
  is not part of the update. A PATCH therefore results in one read and one write at the broker, independent of the
  number of attributes or list items. If the broker does not write all attributes, the PATCH is answered with `400`.
  Two concurrent PATCHes that replace the same list can end with the items of both.

| Broker | `replaceOnUpdate` | Behaviour |
|--------|-------------------|-----------|
| **Orion-LD** | `false` (default) | Orion-LD neither creates attributes with `PATCH /attrs` nor accepts the NGSI-LD Null to delete a single attribute instance. Attributes and list items of the PATCH are written, but list items that are no longer part of the PATCH stay stored, and empty lists are not supported. |
| **Scorpio < 6.0.0** | `false` (default) | Appending replaces array attributes in place. Scorpio < 6.0.0 does not create attributes with `PATCH /attrs`, so with the `scorpio` profile, which sets `true`, it has to be set back to `false` (e.g. `GENERAL_REPLACE_ON_UPDATE=false`). |
| **Scorpio >= 6.0.0** | `true` | Required. Appending keeps the existing items of array attributes. Each module's `application-scorpio.yaml` already sets it. |

```yaml
# application-scorpio.yaml (already included in each module)
general:
  replaceOnUpdate: true
```

## References to other deployments

A reference whose `href` names one of the hosts in `references.external-hosts` (`REFERENCES_EXTERNAL_HOSTS`) is
external and is not validated against local storage. The list is empty by default, so every reference is validated.
This deployment's own hosts (`general.serverHost`, `references.local-hosts`/`REFERENCES_LOCAL_HOSTS`) stay local
even when they are listed. See [common/README.md](common/README.md#local-and-external-references).

## Extension with @schemaLocation

TMForum supports the extension of API entities through the ```@schemaLocation``` property. In order to extend an entity,
the property should contain a valid(and reachable) URL to a [JSON-Schema](https://json-schema.org/), defining the
extension.
When a schema is provided, the entity is validated against it during deserialization on insertion or update.
For example, in order to extend the ```ProductOfferingTerm``` with a policy, the following schema could be used:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "http://localhost:3000/ProductOfferingTerm-schema.json",
  "title": "Test Schema - Extension with a policy",
  "description": "Test Schema - Extension with a policy",
  "type": "object",
  "properties": {
    "policy": {
      "type": "object"
    }
  },
  "required": [
    "policy"
  ]
}
```

With that schema, a product offering could be created as following:

```json
{
  "id": "string",
  "href": "string",
  "description": "string",
  "productOfferingTerm": [
    {
      "description": "string",
      "name": "string",
      "validFor": {
        "endDateTime": "1985-04-12T23:20:50.52Z",
        "startDateTime": "1985-04-12T23:20:50.52Z"
      },
      "policy": {
        "the": "policy"
      },
      "@baseType": "ProductOfferingTerm",
      "@schemaLocation": "http://localhost:3000/ProductOfferingTerm-schema.json",
      "@type": "ProductOfferingTermWithPolicy"
    }
  ]
}
```

The API will validate the policy is present and actually an object. Its possible to define in detail the properties of
the policy itself and let them be validated, too.

Notes:

* All schemas need to define there meta-schema. When using the meta-schemas from ```json-schema.org```, only
  ```draft/2020-12/schema```, ```draft/2019-09/schema```, ```draft-04/schema```, ```draft-06/schema``` and
  ```draft-07/schema``` are allowed.
* It is possible to set ```additionalProperties: false``` for the object to be extended. However, in this case the
  schema needs to contain the full definition of the object, including the properties from the baseType. Else, they will
  be considered as additional properties and validation will fail.  

### Caching of the schemas

Schemas are downloaded from the address given in ```@schemaLocation```. To prevent a download on every single request,
they are cached in-memory. The cache is bound by a time-to-live - e.g. changes at the schema provider are picked up
after at most that time - and by the number of cached schemas, to prevent clients from filling up the memory through
arbitrary schema addresses. The least recently used schemas are evicted first and failed downloads are not cached.

| Property                            | Default | Description                                                                                            |
|-------------------------------------|---------|--------------------------------------------------------------------------------------------------------|
| ```general.schema-cache.enabled```     | true    | Enables the caching of downloaded schemas. If disabled, every validation downloads the schema again.     |
| ```general.schema-cache.time-to-live```| 30m     | Time a downloaded schema is kept, before it is downloaded again. A duration of ```0s``` disables caching. |
| ```general.schema-cache.max-size```    | 100     | Maximum number of schemas to be kept in the cache.                                                       |

```yaml
general:
  schema-cache:
    enabled: true
    time-to-live: 30m
    max-size: 100
```

## Release notes

|Version| Notes link                                                                                 |
|------|--------------------------------------------------------------------------------------------|
| 1.5.0 | [Add TMF652, TMF641, TMF730](./release-notes/RELEASE_NOTES_ADD_NEW_TMFORUM_API_MODULES.md) |