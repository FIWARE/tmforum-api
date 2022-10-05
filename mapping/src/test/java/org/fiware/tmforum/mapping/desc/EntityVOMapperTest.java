package org.fiware.tmforum.mapping.desc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Single;
import org.fiware.ngsi.model.AdditionalPropertyVO;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.mapping.AdditionalPropertyMixin;
import org.fiware.tmforum.mapping.EntitiesRepository;
import org.fiware.tmforum.mapping.EntityVOMapper;
import org.fiware.tmforum.mapping.desc.pojos.MyPojoWithSubEntity;
import org.fiware.tmforum.mapping.desc.pojos.MyPojoWithSubEntityEmbed;
import org.fiware.tmforum.mapping.desc.pojos.MySubProperty;
import org.fiware.tmforum.mapping.desc.pojos.MySubPropertyEntity;
import org.fiware.tmforum.mapping.desc.pojos.MySubPropertyEntityEmbed;
import org.fiware.tmforum.mapping.desc.pojos.PropertyListPojo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}