# FIWARE implementation of the TMFORUM-APIs


## Structure

The project is setup as a maven multi-module project, to ease the development of independent implementations for each API. 
There are currently two supporting modules:
- [common](common) - providing functionality that most of the implementations will require
- [mapping-module](mapping) - providing an annotation based mapping between NGSI-LD objects and Java-Pojos to support the usage of a decoupling-layer.

The api-implementations are inside modules, producing jar-files and oci-containers according to the definitions in the [parent-pom](pom.xml). 
Current api-implementations:
- [party-management-api](party) - implementation of the party-management-api
- [customer-management-api](customer) - implementation of the customer-management-api

The project also contains 2 non-module folders:
- [api](api) - contains the [OpenApi-Specifications](https://spec.openapis.org/oas/v3.1.0) used by the project. Beside the TMForum-Apis it also contains the specification of the NGSI-LD API.
- [k3s](k3s) - contains kubernetes manifests to be used in the integration-test environments or for setting up a dev-environment. Currently includes:
  - [Orion-LD Context Broker](https://github.com/FIWARE/context.Orion-LD)
  - [MongoDB](https://www.mongodb.com/)
  - 
## Mapping between TMFORUM Objects and NGSI-LD

In order to ease the implementation of the apis, a mapper from java-objects to NGSI-LD objects is part of the project:
- [mapping-module](mapping)

## Testing

The current implementation supports multiple layers of testing:
- Unit-Tests, using [JUnit5](https://junit.org/junit5/docs/current/user-guide/) and [Mockito](https://site.mockito.org/) to test on a function-level
- Unit-Tests, using [JUnit5](https://junit.org/junit5/docs/current/user-guide/),[Micronaut-Test](https://micronaut-projects.github.io/micronaut-test/latest/guide/) and [Mockito](https://site.mockito.org/) to test on component level(e.g. running parts of the application, while mocking all external dependencies)
- Integration-Tests, using [JUnit5](https://junit.org/junit5/docs/current/user-guide/),[Micronaut-Test](https://micronaut-projects.github.io/micronaut-test/latest/guide/) and [k3s](https://k3s.io/) to test the application with its real external dependencies running

### Running unit-tests

The unit-tests are integrated into the [maven-lifecycle's](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html) ```test-phase``` via the [surfire-plugin](https://maven.apache.org/surefire/maven-surefire-plugin/).
They can be executed via ```mvn test```. Since they don't have any external dependencies, they also can be run via the IDEs(only tested with [IntelliJ](https://www.jetbrains.com/idea/)) integrated test runners.
All tests with suffix ```*Test``` will be executed.

### Running integration-tests

The integration-tests are integrated into the [maven-lifecycle's](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html) ```integration-test phase``` via the [failsafe-plugin](https://maven.apache.org/surefire/maven-failsafe-plugin/). Since the 
tests require a running broker at ```localhost:1026```, the [k3s-plugin](https://github.com/kokuwaio/k3s-maven-plugin) is integrated in the ```pre-integration-test``` and ```post-intgeration-test``` phases to spin-up a k3s-cluster inside docker and deploy the components.
The tests can be executed via: ```mvn integration-test```. After running the tests, the k3s-cluster will be automatically destroyed.
All tests with suffix ```*IT``` will be executed.

In order to run the tests from the IDE, you have to setup an environment before that. This can be achieved via the ```dev```-[profile](https://maven.apache.org/guides/introduction/introduction-to-profiles.html).
With ```mvn install -Pdev```, a k3s-cluster will be setup but not destroyed after running the tests. 
In order to connect to the cluster, install the [kubectl-client](https://kubernetes.io/docs/tasks/tools/#kubectl) and run ```export KUBECONFIG=/tmp/k3s-maven-plugin/mount/kubeconfig.yaml && kubectl get all --all-namespaces```.
To clean up the environment, use ```mvn clean k3s:rm -Pdev```.

## Dev-Environment

To get a dev-environment, the [k3s-plugin](https://github.com/kokuwaio/k3s-maven-plugin) can be used: ```mvn install -Pdev```
After running this, the context broker is available at ```http://localhost:1026```
See the logs via:
```
  export KUBECONFIG=/tmp/k3s-maven-plugin/mount/kubeconfig.yaml && kubectl get all --all-namespaces
  kubectl logs <BROKER_POD>
```
