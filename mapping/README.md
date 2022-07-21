# Mapping between Java-Objects and NGSI-LD entities

In order to integrate Service-APIs and there data-models with the NGSI-LD API, a mapping mechanism between the two worlds is required. This mapping module intents to provide that. Similar to other mapping mechanisms
like [jackson](https://github.com/FasterXML/jackson) for JSON-Java,
[mapstruct](https://mapstruct.org/) for Java-Java or [Hibernate](https://hibernate.org/) for Java-Database, it uses an annotation-based approach. A set of specific [annotations](src/main/java/org/fiware/tmforum/mapping/annotations) is
provided, that needs to be used for describing the object mappings.

## Usage

In order to enable the mappers to translate correctly between both worlds, the domain object has to be annotated properly.

> :bulb: All examples have a test running the described scenario. You should look up all the details there.
> The tests can be found under the [test-folder](src/test/java/org/fiware/tmforum/mapping/desc/), the ```DisplayName``` corresponds
> with the example name referenced in the :mag: block.

### Simple example


Lets start with a simple example:

> :mag: [JavaObjectMapperTest](src/test/java/org/fiware/tmforum/mapping/desc/JavaObjectMapperTest.java) - Simple pojo mapping.

```java
@MappingEnabled(value = "my-pojo")
public class MyPojo {
	private static final String ENTITY_TYPE = "my-pojo";

	private URI id;

	private String myName;
	private List<Integer> numbers;

	public MyPojo(String id) {
		this.id = URI.create(id);
	}

	@EntityId
	public URI getId() {
		return id;
	}

	@EntityType
	public String getType() {
		return ENTITY_TYPE;
	}

	@AttributeGetter(value = AttributeType.PROPERTY, targetName = "name")
	public String getMyName() {
		return myName;
	}

	@AttributeSetter(value = AttributeType.PROPERTY, targetName = "name")
	public void setMyName(String myName) {
		this.myName = myName;
	}

	@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "numbers")
	public List<Integer> getNumbers() {
		return numbers;
	}

	@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "numbers", targetClass = Integer.class)
	public void setNumbers(List<Integer> numbers) {
		this.numbers = numbers;
	}
}
```

This now can be translated with the [JavaObjectMapper](src/main/java/org/fiware/tmforum/mapping/JavaObjectMapper.java) to an EntityVO that is compliant with
the [NGSI-LD](https://docbox.etsi.org/isg/cim/open/Latest%20release%20NGSI-LD%20API%20for%20public%20comment.pdf) data-model.

```json
{
  "@context": "https://smartdatamodels.org/context.jsonld",
  "id": "urn:ngsi-ld:my-pojo:the-test-pojo",
  "type": "my-pojo",
  "numbers": {
    "type": "Property",
    "value": [
      1,
      2,
      3
    ]
  },
  "name": {
    "type": "Property",
    "value": "The test pojo."
  }
}
```

To explain all the bits:

* The class requires to:
    * be annotated with [@MappingEnabled](src/main/java/org/fiware/tmforum/mapping/annotations/MappingEnabled.java). The annotation has to provide the potential types of entitys is can be translated to.
    * have a single-string constructor. This is used to recreate it after reading out from the broker. The constructor is responsible for setting the entity-id.
    * have a method annotated with [@EntityId](src/main/java/org/fiware/tmforum/mapping/annotations/EntityId.java), that has to return the ID of the entity as a URI.
    * have a method annotated with [@EntityType](src/main/java/org/fiware/tmforum/mapping/annotations/EntityType.java), that has to return the type of the entity in NGSI-LD as a string
* In order to get properties set to the entity the method-level annotations [@AttributeGetter](src/main/java/org/fiware/tmforum/mapping/annotations/AttributeGetter.java)
  and [@AttributeSetter](src/main/java/org/fiware/tmforum/mapping/annotations/AttributeSetter.java) are important:
    * the field needs to be accessible via methods, following the getter/setter-pattern.
    * the property in NGSI-LD can be named different than the field(f.e. to comply with a [SmartDataModel](https://smartdatamodels.org/)), use the ```targetName``` for that
    * getter:
        * the annotation needs to specify the type of target attribute in NGSI-LD via [AttributeType](src/main/java/org/fiware/tmforum/mapping/annotations/AttributeType.java)
        * specify the name of the property in NGSI-LD
    * setter:
        * the annotation needs to specify the type of target attribute in NGSI-LD via [AttributeType](src/main/java/org/fiware/tmforum/mapping/annotations/AttributeType.java)
        * specify the name of the property in NGSI-LD
        * in case of list-properties, the type of the target-list entries since the generic type is not available on runtime due to [type-erasure](https://en.wikipedia.org/wiki/Type_erasure)

This will translate the object to an entity and allows translate it back via the [EntityVOMapper](src/main/java/org/fiware/tmforum/mapping/EntityVOMapper.java).

### Advanced topics

The mapping-module intends to support complex data-structures and the usage of most(if not all) features of the NGSI-LD data-model. The following documentation will go through the features and describe it.

> :bulb: In order to make the examples more readable, they will not include the full methods anymore. They will use [lombok-annotations](https://projectlombok.org/) to generate the actual methods.

#### Objects as properties

When an individual object is used inside the entity, it can be embedded as a property:

> :mag: [JavaObjectMapperTest](src/test/java/org/fiware/tmforum/mapping/desc/JavaObjectMapperTest.java) - Map Pojo with a field that is an object.
```java
@Data
public class MySubProperty {

	private String propertyName;
}

@MappingEnabled(entityType = "complex-pojo")
public class MyPojoWithSubProperty(String id) {

	@Getter(onMethod = @__({@EntityId}))
	private URI id;

	@Getter(onMethod = @__({@EntityType}))
	private String type = "complex-pojo";

	public MyPojoWithSubProperty(String id) {
		this.id = URI.create(id);
	}

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "mySubProperty")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "mySubProperty")}))
	private MySubProperty mySubProperty;
}
```
The sub-property will be translated into a plain json-object: 

```json
{
  "@context": "https://smartdatamodels.org/context.jsonld",
  "id": "urn:ngsi-ld:complex-pojo:the-test-pojo",
  "type": "complex-pojo",
  "mySubProperty": {
    "type": "Property",
    "value": {
      "propertyName": "My property"
    }
  }
}
```

The sub-property also can be a list of objects, too:
> :mag: [JavaObjectMapperTest](src/test/java/org/fiware/tmforum/mapping/desc/JavaObjectMapperTest.java) - Map Pojo with a field that is a list of objects.
```java
@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "mySubProperty")}))
@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "mySubProperty", targetClass = MySubProperty.class)}))
private List<MySubProperty> mySubProperties;
```

Note that the setter-annotation now has to contain the value ```targetClass``` to provide the generic-type of the list. 

```json
{
	"@context": "https://smartdatamodels.org/context.jsonld",
	"id": "urn:ngsi-ld:complex-pojo:the-test-pojo",
	"type": "complex-pojo",
	"mySubProperty": {
		"type": "Property",
		"value": [{
			"propertyName": "My property 1"
		}, {
			"propertyName": "My property 2"
		}]
	}
}
```

> :warning: One thing that jumps out in terms of NGSI-LD: this is not a real list of properties, but a list of objects of the property.
> That translation is done, since a real list of properties is only possible if a ```DatasetId``` exists. Since this cannot be guaranteed for every datamodel
> the decision was made to keep it as list of objects.

### Relationships on mapping to NGSI-LD

NGSI-LD is a lot about relating entities. For objects, we typically want the same. Therefor the [AttributeType's](src/main/java/org/fiware/tmforum/mapping/annotations/AttributeType.java) ```RELATIONSHIP``` and ```RELATIONSHIP_LIST``` exist.
In the most simple example, the object defined as a sub-property can be another entity:

> :mag: [JavaObjectMapperTest](src/test/java/org/fiware/tmforum/mapping/desc/JavaObjectMapperTest.java) - Map Pojo with a field that is a relationship.
```java
@MappingEnabled(entityType = "sub-entity")
public class MySubPropertyEntity {

	@Getter(onMethod = @__({@EntityId, @RelationshipObject, @DatasetId}))
	private URI id;

	@Getter(onMethod = @__({@EntityType}))
	private String type = "sub-entity";

	public MySubPropertyEntity(String id) {
		this.id = URI.create(id);
	}

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "name")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "name")}))
	private String name = "myName";
}
```

The class itself has only two differences to the already known annotations:
* the ```@RelationshipObject```-annotation. Since a relationship has an ```object``` referencing the other 
    entity, it has to specify how to get that object(which most of the time will be the entity-id).
* the ```@DatasetId```-annotation. In order to allow lists of relationships, we have to provide a datasetId to NGSI-LD.
  In case of relationships, this can easily be solved by using the id. An additional annotation was provided, to allow 
  different approaches to the dataset.
The reference on the parent entity looks as following:
```java
	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "sub-entity")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "sub-entity", targetClass = MySubPropertyEntity.class)}))
	private MySubPropertyEntity mySubProperty;
```
The only difference to the already known is the ```AttributeType.RELATIONSHIP```. It tells the mapper to create a relationship, instead of a property.
The resulting object looks as following:
```json
{
	"@context": "https://smartdatamodels.org/context.jsonld",
	"id": "urn:ngsi-ld:complex-pojo:the-test-pojo",
	"type": "complex-pojo",
	"sub-entity": {
		"object": "urn:ngsi-ld:sub-entity:the-sub-entity",
		"type": "Relationship",
		"datasetId": "urn:ngsi-ld:sub-entity:the-sub-entity"
	}
}
```

In order to provide properties of a relationship, the ```embedProperty``` field of the [@AttributeGetter](src/main/java/org/fiware/tmforum/mapping/annotations/AttributeGetter.java) can be used. It adds the field to the object:
The sub-entity contains another field:
> :mag: [JavaObjectMapperTest](src/test/java/org/fiware/tmforum/mapping/desc/JavaObjectMapperTest.java) - Map Pojo with a field that is a relationship with additional attributes.
```java
@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "role", embedProperty = true)}))
@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "role")}))
private String role = "Sub-Entity";
```

This role will be added to the relationship:
```json
{
  "@context": "https://smartdatamodels.org/context.jsonld",
  "id": "urn:ngsi-ld:complex-pojo:the-test-pojo",
  "type": "complex-pojo",
  "sub-entity": {
    "object": "urn:ngsi-ld:sub-entity:the-sub-entity",
    "type": "Relationship",
    "datasetId": "urn:ngsi-ld:sub-entity:the-sub-entity",
    "role": {
      "type": "Property",
      "value": "Sub-Entity"
    }
  }
}
```

### Relationships on mapping from NGSI-LD

To reading a relationship back from NGSI-LD is also possible. The ```EntityVOMapper``` therefor requires an implementation of the [EntitiesRepository](src/main/java/org/fiware/tmforum/mapping/EntitiesRepository.java)
to retrieve the actual entities related from NGSI-LD.
> :warning: In order to use that feature, referential integrity has to be assured. The referenced entities need to be provided through the repository-interface.
 
In case of the previous example: 
> :mag: [EntityVOMapperTest](src/test/java/org/fiware/tmforum/mapping/desc/EntityVOMapperTest.java) - Map entity containing a relationship.
```json
{
	"@context": "https://smartdatamodels.org/context.jsonld",
	"id": "urn:ngsi-ld:complex-pojo:the-test-pojo",
	"type": "complex-pojo",
	"sub-entity": {
		"object": "urn:ngsi-ld:sub-entity:the-sub-entity",
		"type": "Relationship",
		"datasetId": "urn:ngsi-ld:sub-entity:the-sub-entity"
	}
}
```
The entity ```urn:ngsi-ld:sub-entity:the-sub-entity``` also has to exist within the broker:
```json
{
	"@context": "https://smartdatamodels.org/context.jsonld",
	"id": "urn:ngsi-ld:sub-entity:the-sub-entity",
	"type": "sub-entity",
	"name": {
		"type": "Property",
		"value": "myName"
	}
}
```
The mapper will then retrieve all entites referenced as a relationship from the repository and generate the defined objects.
Remember the annotation-value ```targetClass```, it will be used to map the referenced entity to the target type:
> :mag: [EntityVOMapperTest](src/test/java/org/fiware/tmforum/mapping/desc/EntityVOMapperTest.java) - Map entity containing a relationship with embedded values.
```java
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "sub-entity", targetClass = MySubPropertyEntity.class)}))
	private MySubPropertyEntity mySubProperty;
```
The resulting object will then look as following(json for better readability):
```json
{
	"id": "urn:ngsi-ld:complex-pojo:the-test-pojo",
	"type": "complex-pojo",
	"mySubProperty": {
		"id": "urn:ngsi-ld:sub-entity:the-sub-entity",
		"type": "sub-entity",
		"name": "myName"
	}
}
```

Reading out embedded properties(again, same example), instead of the related entity achieved(f.e. if a generic object is used):
```json
{
  "@context": "https://smartdatamodels.org/context.jsonld",
  "id": "urn:ngsi-ld:complex-pojo:the-test-pojo",
  "type": "complex-pojo",
  "sub-entity": {
    "object": "urn:ngsi-ld:sub-entity:the-sub-entity",
    "type": "Relationship",
    "datasetId": "urn:ngsi-ld:sub-entity:the-sub-entity",
    "role": {
      "type": "Property",
      "value": "Sub-Entity"
    }
  }
}
```
With the value ```fromProperties=true``` on the [@AttributeSetter](src/main/java/org/fiware/tmforum/mapping/annotations/AttributeGetter.java), 
the mapper will read out the properties and put them in the object:  

```java
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "sub-entity", fromProperties = true)}))
    private MySubPropertyEntityEmbed mySubProperty;
```
The result is an object, containing values from the parent and the actual entity:
```json
{
	"id": "urn:ngsi-ld:complex-pojo:the-test-pojo",
	"type": "complex-pojo",
	"mySubProperty": {
		"id": "urn:ngsi-ld:sub-entity:the-sub-entity",
		"type": "sub-entity",
		"name": "myName",
		"role": "Sub-Entity"
	}
}
```




