package org.fiware.tmforum.common.mapping;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.fiware.tmforum.common.exception.SchemaValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidatingDeserializerTest {

	private ObjectMapper objectMapper;

	/**
	 * Simulates a regular VO with a field whose raw Java name (atSchemaLocation) differs from its
	 * Jackson-serialized name (@schemaLocation).
	 */
	static class TestVO extends UnknownPreservingBase {
		@JsonProperty("@schemaLocation")
		private URI atSchemaLocation;

		@JsonProperty("name")
		private String name;

		@Override
		public URI getAtSchemaLocation() {
			return atSchemaLocation;
		}

		public void setAtSchemaLocation(URI atSchemaLocation) {
			this.atSchemaLocation = atSchemaLocation;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.setDeserializerModifier(new BeanDeserializerModifier() {
			@Override
			public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config,
														  BeanDescription beanDescription,
														  JsonDeserializer<?> originalDeserializer) {
				return new ValidatingDeserializer(originalDeserializer, beanDescription, true);
			}
		});
		objectMapper.registerModule(module);
	}

	/**
	 * A schema that defines "@schemaLocation" (the Jackson property name) should be detected as
	 * attempting to override a base property and must throw.
	 */
	@Test
	void schemaDefiningJsonPropertyNameShouldBeRejected() {
		String json = """
				{"name":"test","@schemaLocation":"classpath:schema/override-json-property.json","customField":"value"}
				""";
		assertThrows(SchemaValidationException.class,
				() -> objectMapper.readValue(json, TestVO.class),
				"A schema defining '@schemaLocation' (the Jackson/JSON property name) must be rejected");
	}

	/**
	 * A schema that defines "atSchemaLocation" (the raw Java field name, not exposed as a JSON property)
	 * must NOT be rejected — raw Java names are not JSON property names and must not be used for the check.
	 * This is the core regression test: the old getAllFieldNames() approach would have incorrectly rejected this.
	 */
	@Test
	void schemaDefiningRawJavaFieldNameShouldNotBeRejected() {
		String json = """
				{"name":"test","@schemaLocation":"classpath:schema/override-raw-field.json","customField":"value"}
				""";
		assertDoesNotThrow(
				() -> objectMapper.readValue(json, TestVO.class),
				"A schema defining 'atSchemaLocation' (raw Java field name) must not be rejected — it is not a JSON property");
	}

	/**
	 * A schema that only adds new custom properties should pass without any exception.
	 */
	@Test
	void validExtensionSchemaShouldPass() {
		String json = """
				{"name":"test","@schemaLocation":"classpath:schema/valid-extension.json","customField":"value"}
				""";
		assertDoesNotThrow(
				() -> objectMapper.readValue(json, TestVO.class),
				"A schema adding only new custom properties must be accepted");
	}

	/**
	 * A schema that defines a conflicting property name inside an "allOf" block should be detected
	 * by the deep traversal and rejected.
	 */
	@Test
	void schemaOverridingPropertyInAllOfShouldBeRejected() {
		String json = """
				{"name":"test","@schemaLocation":"classpath:schema/override-in-allof.json","customField":"value"}
				""";
		assertThrows(SchemaValidationException.class,
				() -> objectMapper.readValue(json, TestVO.class),
				"A schema overriding 'name' inside an 'allOf' block must be rejected via deep traversal");
	}

}
