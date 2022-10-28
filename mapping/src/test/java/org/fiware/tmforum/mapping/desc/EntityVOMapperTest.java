package org.fiware.tmforum.mapping.desc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.source.doctree.SeeTree;
import io.reactivex.Single;
import org.fiware.ngsi.model.AdditionalPropertyVO;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.PropertyVO;
import org.fiware.ngsi.model.RelationshipListVO;
import org.fiware.ngsi.model.RelationshipVO;
import org.fiware.tmforum.mapping.AdditionalPropertyMixin;
import org.fiware.tmforum.mapping.EntitiesRepository;
import org.fiware.tmforum.mapping.EntityVOMapper;
import org.fiware.tmforum.mapping.MappingException;
import org.fiware.tmforum.mapping.desc.pojos.MyPojo;
import org.fiware.tmforum.mapping.desc.pojos.MyPojoWithSubEntity;
import org.fiware.tmforum.mapping.desc.pojos.MyPojoWithSubEntityEmbed;
import org.fiware.tmforum.mapping.desc.pojos.MyPojoWithSubEntityFrom;
import org.fiware.tmforum.mapping.desc.pojos.MyPojoWithSubEntityListFrom;
import org.fiware.tmforum.mapping.desc.pojos.MySubProperty;
import org.fiware.tmforum.mapping.desc.pojos.MySubPropertyEntity;
import org.fiware.tmforum.mapping.desc.pojos.MySubPropertyEntityEmbed;
import org.fiware.tmforum.mapping.desc.pojos.MySubPropertyEntityWithWellKnown;
import org.fiware.tmforum.mapping.desc.pojos.PropertyListPojo;
import org.fiware.tmforum.mapping.desc.pojos.invalid.MyPojoWithSubEntityWellKnown;
import org.fiware.tmforum.mapping.desc.pojos.invalid.MyPojoWithWrongConstructor;
import org.fiware.tmforum.mapping.desc.pojos.invalid.MySetterThrowingPojo;
import org.fiware.tmforum.mapping.desc.pojos.invalid.MyThrowingConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EntityVOMapperTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private EntityVOMapper entityVOMapper;
    private EntitiesRepository entitiesRepository = mock(EntitiesRepository.class);

    @BeforeEach
    public void setup() {
        entityVOMapper = new EntityVOMapper(OBJECT_MAPPER, entitiesRepository);
        OBJECT_MAPPER
                .addMixIn(AdditionalPropertyVO.class, AdditionalPropertyMixin.class);
    }

    @DisplayName("Map entity containing a relationship.")
    @Test
    void testSubEntityMapping() throws JsonProcessingException {
        MySubPropertyEntity expectedSubEntity = new MySubPropertyEntity("urn:ngsi-ld:sub-entity:the-sub-entity");
        MyPojoWithSubEntity expectedPojo = new MyPojoWithSubEntity("urn:ngsi-ld:complex-pojo:the-test-pojo");
        expectedPojo.setMySubProperty(expectedSubEntity);


        String subEntityString = "{\"@context\":\"https://smartdatamodels.org/context.jsonld\",\"id\":\"urn:ngsi-ld:sub-entity:the-sub-entity\",\"type\":\"sub-entity\",\"name\":{\"type\":\"Property\",\"value\":\"myName\"}}";
        EntityVO subEntity = OBJECT_MAPPER.readValue(subEntityString, EntityVO.class);

        when(entitiesRepository.getEntities(anyList())).thenReturn(Mono.just(List.of(subEntity)));

        String parentEntityString = "{\"@context\":\"https://smartdatamodels.org/context.jsonld\",\"id\":\"urn:ngsi-ld:complex-pojo:the-test-pojo\",\"type\":\"complex-pojo\",\"sub-entity\":{\"object\":\"urn:ngsi-ld:sub-entity:the-sub-entity\",\"type\":\"Relationship\",\"datasetId\":\"urn:ngsi-ld:sub-entity:the-sub-entity\"}}";
        EntityVO parentEntity = OBJECT_MAPPER.readValue(parentEntityString, EntityVO.class);

        MyPojoWithSubEntity myPojoWithSubEntity = entityVOMapper.fromEntityVO(parentEntity, MyPojoWithSubEntity.class).block();
        assertEquals(expectedPojo, myPojoWithSubEntity, "The full pojo should be retrieved.");
    }

    @DisplayName("Map entity containing a relationship with embedded values.")
    @Test
    void testSubEntityEmbedMapping() throws JsonProcessingException {
        MySubPropertyEntityEmbed expectedSubEntity = new MySubPropertyEntityEmbed("urn:ngsi-ld:sub-entity:the-sub-entity");
        MyPojoWithSubEntityEmbed expectedPojo = new MyPojoWithSubEntityEmbed("urn:ngsi-ld:complex-pojo:the-test-pojo");
        expectedPojo.setMySubProperty(expectedSubEntity);

        String subEntityString = "{\"@context\":\"https://smartdatamodels.org/context.jsonld\",\"id\":\"urn:ngsi-ld:sub-entity:the-sub-entity\",\"type\":\"sub-entity\",\"name\":{\"type\":\"Property\",\"value\":\"myName\"}}";
        EntityVO subEntity = OBJECT_MAPPER.readValue(subEntityString, EntityVO.class);

        when(entitiesRepository.getEntities(anyList())).thenReturn(Mono.just(List.of(subEntity)));

        String parentEntityString = "{\"@context\":\"https://smartdatamodels.org/context.jsonld\",\"id\":\"urn:ngsi-ld:complex-pojo:the-test-pojo\",\"type\":\"complex-pojo\",\"sub-entity\":{\"object\":\"urn:ngsi-ld:sub-entity:the-sub-entity\",\"type\":\"Relationship\",\"datasetId\":\"urn:ngsi-ld:sub-entity:the-sub-entity\",\"role\":{\"type\":\"Property\",\"value\":\"Sub-Entity\"}}}";
        EntityVO parentEntity = OBJECT_MAPPER.readValue(parentEntityString, EntityVO.class);

        MyPojoWithSubEntityEmbed myPojoWithSubEntityEmbed = entityVOMapper.fromEntityVO(parentEntity, MyPojoWithSubEntityEmbed.class).block();
        assertEquals(expectedPojo, myPojoWithSubEntityEmbed, "The full pojo should be retrieved.");
    }

    @DisplayName("Map entity with all supported attribute types.")
    @Test
    void testListEntityMapping() throws JsonProcessingException {
        PropertyListPojo propertyListPojo = new PropertyListPojo("urn:ngsi-ld:list-pojo:the-pojo");

        MySubPropertyEntity subEntity1 = new MySubPropertyEntity("urn:ngsi-ld:sub-entity:the-sub-entity-1");
        MySubPropertyEntity subEntity2 = new MySubPropertyEntity("urn:ngsi-ld:sub-entity:the-sub-entity-2");

        MySubProperty property1 = new MySubProperty();
        property1.setPropertyName("p-1");
        MySubProperty property2 = new MySubProperty();
        property2.setPropertyName("p-2");

        propertyListPojo.setProperty(property1);
        propertyListPojo.setRelationShip(subEntity1);
        propertyListPojo.setPropertyList(List.of(property1, property2));
        propertyListPojo.setRelationshipList(List.of(subEntity1, subEntity2));

        String subEntity1String = "{\"@context\":\"https://smartdatamodels.org/context.jsonld\",\"id\":\"urn:ngsi-ld:sub-entity:the-sub-entity-1\",\"type\":\"sub-entity\",\"name\":{\"type\":\"Property\",\"value\":\"myName\"}}";
        String subEntity2String = "{\"@context\":\"https://smartdatamodels.org/context.jsonld\",\"id\":\"urn:ngsi-ld:sub-entity:the-sub-entity-2\",\"type\":\"sub-entity\",\"name\":{\"type\":\"Property\",\"value\":\"myName\"}}";

        EntityVO parsedSubEntity1 = OBJECT_MAPPER.readValue(subEntity1String, EntityVO.class);
        EntityVO parsedSubEntity2 = OBJECT_MAPPER.readValue(subEntity2String, EntityVO.class);

        when(entitiesRepository.getEntities(anyList())).thenReturn(Mono.just(List.of(parsedSubEntity1, parsedSubEntity2)));

        String parentEntityString = "{\n" +
                "\t\"@context\": \"https://smartdatamodels.org/context.jsonld\",\n" +
                "\t\"id\": \"urn:ngsi-ld:list-pojo:the-pojo\",\n" +
                "\t\"type\": \"list-pojo\",\n" +
                "\t\"mySubProperty\": {\n" +
                "\t  \"value\": {\n" +
                "\t\t\"propertyName\": \"p-1\"\n" +
                "\t  },\n" +
                "\t  \"type\": \"Property\"\n" +
                "\t},\n" +
                "\t\"myRelationship\": {\n" +
                "\t\t\"object\": \"urn:ngsi-ld:sub-entity:the-sub-entity-1\",\n" +
                "\t\t\"type\": \"Relationship\",\n" +
                "\t\t\"datasetId\": \"urn:ngsi-ld:sub-entity:the-sub-entity-1\"\n" +
                "\t},\n" +
                "\t\"mySubPropertyList\": [\n" +
                "\t \t{\n" +
                "\t\t \"value\": {\n" +
                "\t\t\t\"propertyName\": \"p-1\"\n" +
                "\t\t  },\n" +
                "\t  \t\"type\": \"Property\"\n" +
                "\t\t}, \n" +
                "\t  \t{\n" +
                "\t\t  \"value\": {\n" +
                "\t\t\t\"propertyName\": \"p-2\"\n" +
                "\t\t  },\n" +
                "\t\t  \"type\": \"Property\"\n" +
                "\t\t}\n" +
                "\t],\n" +
                "\t\"myRelationshipList\": [\n" +
                "\t \t{\n" +
                "\t\t  \"object\": \"urn:ngsi-ld:sub-entity:the-sub-entity-1\",\n" +
                "\t\t  \"type\": \"Relationship\",\n" +
                "\t\t  \"datasetId\": \"urn:ngsi-ld:sub-entity:the-sub-entity-1\"\n" +
                "\t\t}, \n" +
                "\t  \t{\n" +
                "\t\t  \"object\": \"urn:ngsi-ld:sub-entity:the-sub-entity-2\",\n" +
                "\t\t  \"type\": \"Relationship\",\n" +
                "\t\t  \"datasetId\": \"urn:ngsi-ld:sub-entity:the-sub-entity-2\"\n" +
                "\t\t}\n" +
                "\t]\n" +
                "}";
        EntityVO parentEntity = OBJECT_MAPPER.readValue(parentEntityString, EntityVO.class);

        PropertyListPojo mappedPojo = entityVOMapper.fromEntityVO(parentEntity, PropertyListPojo.class).block();
        assertEquals(propertyListPojo, mappedPojo, "The full pojo should be retrieved.");
    }

    @DisplayName("Only mapping to classes with mapping enabled is supported.")
    @Test
    void failWithoutMappingEnabled() {
        assertThrows(MappingException.class, () -> entityVOMapper.fromEntityVO(new EntityVO(), Object.class).block(), "Only mapping to classes with mapping enabled is supported.");
    }

    @DisplayName("Only mapping to matching classes is supported.")
    @Test
    void failWithoutMatchingClass() {
        EntityVO myEntity = new EntityVO().type("my-type");
        assertThrows(MappingException.class, () -> entityVOMapper.fromEntityVO(myEntity, MyPojo.class).block(), "Only mapping to matching classes is supported.");
    }

    @DisplayName("The target classes should provide a string constructor.")
    @Test
    void failWithoutWrongConstructor() {
        when(entitiesRepository.getEntities(anyList())).thenReturn(Mono.just(List.of()));

        EntityVO myEntity = new EntityVO().type("my-pojo").id(URI.create("urn:ngsi-ld:pojo:pojo"));
        assertThrows(MappingException.class, () -> entityVOMapper.fromEntityVO(myEntity, MyPojoWithWrongConstructor.class).block(), "The target classes should provide a string constructor.");
    }

    @DisplayName("Unmapped properties should be ignored.")
    @Test
    void ignoreUnmappedProperties() {
        MySubPropertyEntity mySubPropertyEntity = new MySubPropertyEntity("urn:ngsi-ld:sub-entity:entity");
        mySubPropertyEntity.setName("non-ignore");
        EntityVO entityVO = new EntityVO().id(URI.create("urn:ngsi-ld:sub-entity:entity")).type("sub-entity");
        entityVO.setAdditionalProperties("non-prop", new PropertyVO().value("ignore"));
        entityVO.setAdditionalProperties("name", new PropertyVO().value("non-ignore"));
        assertEquals(mySubPropertyEntity, entityVOMapper.fromEntityVO(entityVO, MySubPropertyEntity.class).block(), "The non-prop should be ignored.");
    }

    @DisplayName("If the constructor is broken, nothing should be mapped.")
    @Test
    void failOnBrokenConstructor() {
        EntityVO entityVO = new EntityVO().id(URI.create("urn:ngsi-ld:throwing-pojo:id")).type("throwing-pojo");
        assertThrows(MappingException.class, () -> entityVOMapper.fromEntityVO(entityVO, MyThrowingConstructor.class).block(), "If the constructor is broken, nothing should be mapped.");
    }

    @DisplayName("The relationship target should have been created from its properties.")
    @Test
    void mapFromProperties() {
        EntityVO parentEntity = new EntityVO().id(URI.create("urn:ngsi-ld:complex-pojo:entity")).type("complex-pojo");
        EntityVO subEntity = new EntityVO().id(URI.create("urn:ngsi-ld:sub-entity:entity")).type("sub-entity");
        RelationshipVO subRel = new RelationshipVO()._object(subEntity.getId());
        subRel.setAdditionalProperties("name", new PropertyVO().value("my-other-name"));
        parentEntity.setAdditionalProperties("mySubProperty", subRel);

        MySubPropertyEntity expectedSub = new MySubPropertyEntity("urn:ngsi-ld:sub-entity:entity");
        expectedSub.setName("my-other-name");
        MyPojoWithSubEntityFrom expectedPojo = new MyPojoWithSubEntityFrom("urn:ngsi-ld:complex-pojo:entity");
        expectedPojo.setMySubProperty(expectedSub);

        assertEquals(expectedPojo, entityVOMapper.fromEntityVO(parentEntity, MyPojoWithSubEntityFrom.class).block(), "The relationship target should have been created from its properties.");
    }


    @DisplayName("The relationship targets should have been created from its properties.")
    @Test
    void mapListFromProperties() {
        EntityVO parentEntity = new EntityVO().id(URI.create("urn:ngsi-ld:complex-pojo:entity")).type("complex-pojo");
        EntityVO subEntity1 = new EntityVO().id(URI.create("urn:ngsi-ld:sub-entity:entity-1")).type("sub-entity");
        EntityVO subEntity2 = new EntityVO().id(URI.create("urn:ngsi-ld:sub-entity:entity-2")).type("sub-entity");
        RelationshipVO subRel1 = new RelationshipVO()._object(subEntity1.getId());
        RelationshipVO subRel2 = new RelationshipVO()._object(subEntity2.getId());

        subRel1.setAdditionalProperties("name", new PropertyVO().value("sub-entity-1"));
        subRel2.setAdditionalProperties("name", new PropertyVO().value("sub-entity-2"));
        RelationshipListVO relationshipVOS = new RelationshipListVO();
        relationshipVOS.add(subRel1);
        relationshipVOS.add(subRel2);
        parentEntity.setAdditionalProperties("mySubProperty", relationshipVOS);

        MySubPropertyEntity expectedSub1 = new MySubPropertyEntity("urn:ngsi-ld:sub-entity:entity-1");
        expectedSub1.setName("sub-entity-1");
        MySubPropertyEntity expectedSub2 = new MySubPropertyEntity("urn:ngsi-ld:sub-entity:entity-2");
        expectedSub2.setName("sub-entity-2");
        MyPojoWithSubEntityListFrom expectedPojo = new MyPojoWithSubEntityListFrom("urn:ngsi-ld:complex-pojo:entity");
        expectedPojo.setMySubProperty(List.of(expectedSub1, expectedSub2));

        assertEquals(expectedPojo, entityVOMapper.fromEntityVO(parentEntity, MyPojoWithSubEntityListFrom.class).block(), "The relationship targets should have been created from its properties.");
    }

    @DisplayName("If the setter is broken, nothing should be constructed.")
    @Test
    void failWithThrowingSetter() {
        EntityVO entity = new EntityVO().id(URI.create("urn:ngsi-ld:my-pojo:entity")).type("my-pojo");
        assertThrows(MappingException.class, () -> entityVOMapper.fromEntityVO(entity, MySetterThrowingPojo.class).block(), "If the setter is broken, nothing should be constructed.");
    }

    @DisplayName("Well known properties should properly be mapped.")
    @Test
    void mapWithWellKnown() {
        EntityVO entityVO = new EntityVO().id(URI.create("urn:ngsi-ld:complex-pojo:entity")).type("complex-pojo");
        EntityVO subEntity = new EntityVO().id(URI.create("urn:ngsi-ld:sub-entity:entity")).type("sub-entity");
        when(entitiesRepository.getEntities(anyList())).thenReturn(Mono.just(List.of(subEntity)));

        RelationshipVO subRel = new RelationshipVO()
                ._object(subEntity.getId())
                .observedAt(Instant.MAX)
                .createdAt(Instant.MAX)
                .modifiedAt(Instant.MAX)
                .datasetId(subEntity.getId())
                .instanceId(URI.create("id"));
        entityVO.setAdditionalProperties("mySubProperty", subRel);

        MySubPropertyEntityWithWellKnown mySubPropertyEntityWithWellKnown = new MySubPropertyEntityWithWellKnown("urn:ngsi-ld:sub-entity:entity");
        mySubPropertyEntityWithWellKnown.setDatasetId("urn:ngsi-ld:sub-entity:entity");
        mySubPropertyEntityWithWellKnown.setInstanceId("id");
        mySubPropertyEntityWithWellKnown.setCreatedAt(Instant.MAX);
        mySubPropertyEntityWithWellKnown.setModifiedAt(Instant.MAX);
        mySubPropertyEntityWithWellKnown.setObservedAt(Instant.MAX);

        MyPojoWithSubEntityWellKnown myPojoWithSubEntityWellKnown = new MyPojoWithSubEntityWellKnown("urn:ngsi-ld:complex-pojo:entity");
        myPojoWithSubEntityWellKnown.setMySubProperty(mySubPropertyEntityWithWellKnown);

        assertEquals(myPojoWithSubEntityWellKnown, entityVOMapper.fromEntityVO(entityVO, MyPojoWithSubEntityWellKnown.class).block(), "Well known properties should properly be mapped.");
    }

}