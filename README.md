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

The project also contains 3 non-module folders:

- [api](api) - contains the [OpenApi-Specifications](https://spec.openapis.org/oas/v3.1.0) of the NGSI-LD API used by
  the project. It does not contain the TMForum-API specs, since they are downloaded on built-time.
- [k3s](k3s) - contains kubernetes manifests to be used in the integration-test environments or for setting up a
  dev-environment. Currently includes:
    - [Orion-LD Context Broker](https://github.com/FIWARE/context.Orion-LD)
    - [MongoDB](https://www.mongodb.com/)
- [conformance-test](conformance-test) - contains a dockerfile to be used for running the TMForum-Conformance Tests
  inside the K3S setup. Also contains a kubernetes manifest for running the api-implementations.

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

TMForum does provide conformance-test tooling for the
API-implementations: [Open API Conformance](https://projects.tmforum.org/wiki/display/API/Open+API+Conformance)
The tests are integrated into the maven lifecycle with the "conformance-test" profile. To execute them, run "mvn clean
install -Pconformance-test --projects common,mapping,<THE_MODULE> -DskipTests -DskipITs". The tests are mounted into a
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
