package org.fiware.tmforum.mapping.desc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Single;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.mapping.EntitiesRepository;
import org.fiware.tmforum.mapping.EntityVOMapper;
import org.fiware.tmforum.mapping.desc.pojos.MyPojoWithSubEntity;
import org.fiware.tmforum.mapping.desc.pojos.MyPojoWithSubEntityEmbed;
import org.fiware.tmforum.mapping.desc.pojos.MySubPropertyEntity;
import org.fiware.tmforum.mapping.desc.pojos.MySubPropertyEntityEmbed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
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
	}

	@DisplayName("Map entity containing a relationship.")
	@Test
	void testSubEntityMapping() throws JsonProcessingException {
		MySubPropertyEntity expectedSubEntity = new MySubPropertyEntity("urn:ngsi-ld:sub-entity:the-sub-entity");
		MyPojoWithSubEntity expectedPojo = new MyPojoWithSubEntity("urn:ngsi-ld:complex-pojo:the-test-pojo");
		expectedPojo.setMySubProperty(expectedSubEntity);


		String subEntityString = "{\"@context\":\"https://smartdatamodels.org/context.jsonld\",\"id\":\"urn:ngsi-ld:sub-entity:the-sub-entity\",\"type\":\"sub-entity\",\"name\":{\"type\":\"Property\",\"value\":\"myName\"}}";
		EntityVO subEntity = OBJECT_MAPPER.readValue(subEntityString, EntityVO.class);

		when(entitiesRepository.getEntities(anyList())).thenReturn(Single.just(List.of(subEntity)));

		String parentEntityString = "{\"@context\":\"https://smartdatamodels.org/context.jsonld\",\"id\":\"urn:ngsi-ld:complex-pojo:the-test-pojo\",\"type\":\"complex-pojo\",\"sub-entity\":{\"object\":\"urn:ngsi-ld:sub-entity:the-sub-entity\",\"type\":\"Relationship\",\"datasetId\":\"urn:ngsi-ld:sub-entity:the-sub-entity\"}}";
		EntityVO parentEntity = OBJECT_MAPPER.readValue(parentEntityString, EntityVO.class);

		MyPojoWithSubEntity myPojoWithSubEntity = entityVOMapper.fromEntityVO(parentEntity, MyPojoWithSubEntity.class).blockingGet();
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

		when(entitiesRepository.getEntities(anyList())).thenReturn(Single.just(List.of(subEntity)));

		String parentEntityString = "{\"@context\":\"https://smartdatamodels.org/context.jsonld\",\"id\":\"urn:ngsi-ld:complex-pojo:the-test-pojo\",\"type\":\"complex-pojo\",\"sub-entity\":{\"object\":\"urn:ngsi-ld:sub-entity:the-sub-entity\",\"type\":\"Relationship\",\"datasetId\":\"urn:ngsi-ld:sub-entity:the-sub-entity\",\"role\":{\"type\":\"Property\",\"value\":\"Sub-Entity\"}}}";
		EntityVO parentEntity = OBJECT_MAPPER.readValue(parentEntityString, EntityVO.class);

		MyPojoWithSubEntityEmbed myPojoWithSubEntityEmbed = entityVOMapper.fromEntityVO(parentEntity, MyPojoWithSubEntityEmbed.class).blockingGet();
		assertEquals(expectedPojo, myPojoWithSubEntityEmbed, "The full pojo should be retrieved.");
	}
}