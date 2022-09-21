package org.fiware.tmforum.mapping.desc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.mapping.JavaObjectMapper;
import org.fiware.tmforum.mapping.desc.pojos.MyPojoWithListOfSubProperty;
import org.fiware.tmforum.mapping.desc.pojos.MyPojoWithSubEntity;
import org.fiware.tmforum.mapping.desc.pojos.MyPojoWithSubEntityEmbed;
import org.fiware.tmforum.mapping.desc.pojos.MyPojoWithSubProperty;
import org.fiware.tmforum.mapping.desc.pojos.MyPojo;
import org.fiware.tmforum.mapping.desc.pojos.MySubProperty;
import org.fiware.tmforum.mapping.desc.pojos.MySubPropertyEntity;
import org.fiware.tmforum.mapping.desc.pojos.MySubPropertyEntityEmbed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaObjectMapperTest {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private JavaObjectMapper javaObjectMapper;

	@BeforeEach
	public void setup() {
		javaObjectMapper = new JavaObjectMapper();
		OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
	}

	@DisplayName("Simple pojo mapping.")
	@Test
	void testSimplePojoMapping() throws JsonProcessingException {
		String expectedJson = "{\"@context\":\"https://smartdatamodels.org/context.jsonld\",\"id\":\"urn:ngsi-ld:my-pojo:the-test-pojo\",\"type\":\"my-pojo\",\"name\":{\"type\":\"Property\",\"value\":\"The test pojo.\"},\"numbers\":{\"type\":\"Property\",\"value\":[1,2,3]}}";

		MyPojo myPojo = new MyPojo("urn:ngsi-ld:my-pojo:the-test-pojo");
		myPojo.setNumbers(List.of(1, 2, 3));
		myPojo.setMyName("The test pojo.");

		EntityVO expectedEntity = OBJECT_MAPPER.readValue(expectedJson, EntityVO.class);
		expectedEntity.setLocation(null);
		expectedEntity.setOperationSpace(null);
		expectedEntity.setObservationSpace(null);

		assertEquals(expectedEntity, javaObjectMapper.toEntityVO(myPojo), "The pojo should have been translated into a valid entity");
	}

	@DisplayName("Map Pojo with a field that is an object.")
	@Test
	void testSubPropertyMapping() throws JsonProcessingException {
		String expectedJson = "{\"@context\":\"https://smartdatamodels.org/context.jsonld\",\"id\":\"urn:ngsi-ld:complex-pojo:the-test-pojo\",\"type\":\"complex-pojo\",\"mySubProperty\":{\"type\":\"Property\",\"value\":{\"propertyName\":\"My property\"}}}";

		MyPojoWithSubProperty myComplexPojo = new MyPojoWithSubProperty("urn:ngsi-ld:complex-pojo:the-test-pojo");
		MySubProperty mySubProperty = new MySubProperty();
		mySubProperty.setPropertyName("My property");
		myComplexPojo.setMySubProperty(mySubProperty);

		assertEquals(expectedJson, OBJECT_MAPPER.writeValueAsString(javaObjectMapper.toEntityVO(myComplexPojo)), "The pojo should have been translated into a valid entity");
	}
	@DisplayName("Map Pojo with a field that is a list of objects.")
	@Test
	void testListOfSubPropertyMapping() throws JsonProcessingException {
		String expectedJson = "{\"@context\":\"https://smartdatamodels.org/context.jsonld\",\"id\":\"urn:ngsi-ld:complex-pojo:the-test-pojo\",\"type\":\"complex-pojo\",\"mySubProperty\":{\"type\":\"Property\",\"value\":[{\"propertyName\":\"My property 1\"},{\"propertyName\":\"My property 2\"}]}}";

		MyPojoWithListOfSubProperty myComplexPojo = new MyPojoWithListOfSubProperty("urn:ngsi-ld:complex-pojo:the-test-pojo");
		MySubProperty mySubProperty1 = new MySubProperty();
		mySubProperty1.setPropertyName("My property 1");
		MySubProperty mySubProperty2 = new MySubProperty();
		mySubProperty2.setPropertyName("My property 2");
		myComplexPojo.setMySubProperties(List.of(mySubProperty1, mySubProperty2));

		assertEquals(expectedJson, OBJECT_MAPPER.writeValueAsString(javaObjectMapper.toEntityVO(myComplexPojo)), "The pojo should have been translated into a valid entity");
	}

	@DisplayName("Map Pojo with a field that is a relationship.")
	@Test
	void testSubEntityMapping() throws JsonProcessingException {
		String expectedJson = "{\"@context\":\"https://smartdatamodels.org/context.jsonld\",\"id\":\"urn:ngsi-ld:complex-pojo:the-test-pojo\",\"type\":\"complex-pojo\",\"sub-entity\":{\"object\":\"urn:ngsi-ld:sub-entity:the-sub-entity\",\"type\":\"Relationship\",\"datasetId\":\"urn:ngsi-ld:sub-entity:the-sub-entity\"}}";

		MySubPropertyEntity mySubProperty = new MySubPropertyEntity("urn:ngsi-ld:sub-entity:the-sub-entity");
		MyPojoWithSubEntity myComplexPojo = new MyPojoWithSubEntity("urn:ngsi-ld:complex-pojo:the-test-pojo");
		myComplexPojo.setMySubProperty(mySubProperty);
		assertEquals(expectedJson, OBJECT_MAPPER.writeValueAsString(javaObjectMapper.toEntityVO(myComplexPojo)), "The pojo should have been translated into a valid entity");
	}

	@DisplayName("Map Pojo with a field that is a relationship with additional attributes.")
	@Test
	void testSubEntityEmbedMapping() throws JsonProcessingException {
		String expectedJson = "{\"@context\":\"https://smartdatamodels.org/context.jsonld\",\"id\":\"urn:ngsi-ld:complex-pojo:the-test-pojo\",\"type\":\"complex-pojo\",\"sub-entity\":{\"object\":\"urn:ngsi-ld:sub-entity:the-sub-entity\",\"type\":\"Relationship\",\"datasetId\":\"urn:ngsi-ld:sub-entity:the-sub-entity\",\"role\":{\"type\":\"Property\",\"value\":\"Sub-Entity\"}}}";

		MySubPropertyEntityEmbed mySubProperty = new MySubPropertyEntityEmbed("urn:ngsi-ld:sub-entity:the-sub-entity");
		MyPojoWithSubEntityEmbed myComplexPojo = new MyPojoWithSubEntityEmbed("urn:ngsi-ld:complex-pojo:the-test-pojo");
		myComplexPojo.setMySubProperty(mySubProperty);
		assertEquals(expectedJson, OBJECT_MAPPER.writeValueAsString(javaObjectMapper.toEntityVO(myComplexPojo)), "The pojo should have been translated into a valid entity");
	}
}