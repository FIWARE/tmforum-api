package org.fiware.tmforum.common.mapping;

import io.github.wistefan.mapping.AdditionalPropertyMixin;
import io.github.wistefan.mapping.CacheSerdeableObjectMapper;
import io.github.wistefan.mapping.NgsiLdMappingModule;
import io.github.wistefan.mapping.mixins.AdditionalPropertyDiscriminatorMixin;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.fiware.ngsi.model.AdditionalPropertyVO;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.GeoPropertyListVO;
import org.fiware.ngsi.model.PropertyListVO;
import org.fiware.ngsi.model.RelationshipListVO;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the Jackson 3 redesign of the mapper customisations ({@link TmForumMappingModule}, {@link FieldCleaningModule},
 * {@code jackson.*} config) end-to-end through the HTTP JSON codec, and keeps them off the cache mapper.
 * The {@code jackson.*} properties are a copy of the block every module's {@code application.yaml} carries, so this
 * proves the keys bind and behave, not that each module's YAML is correct.
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MapperCodecRegressionTest implements TestPropertyProvider {

	public static final String SPEC_NAME = "MapperCodecRegressionTest";

	private static final String SCHEMA_LOCATION = "classpath:schemas/codec-test-extension.json";
	private static final String CACHED_ENTITY_TEMPLATE = "{\"id\":\"urn:ngsi-ld:my-pojo:cache\",\"type\":\"my-pojo\",%s}";
	// set by SchemaValidationExceptionHandler, proves the rejection is not a generic decoding error
	private static final String SCHEMA_VALIDATION_MESSAGE_PREFIX = "Schema validation failed";
	private static final String MESSAGE_FIELD = "message";
	// the context mapper is under test, so the expectations are parsed independently
	private static final ObjectMapper EXPECTATION_MAPPER = JsonMapper.builder().build();

	@Inject
	@Client("/")
	private HttpClient httpClient;
	@Inject
	private ObjectMapper objectMapper;
	@Inject
	private CacheSerdeableObjectMapper cacheMapper;

	@Override
	public Map<String, String> getProperties() {
		return Map.of(
				StubBrokerController.SPEC_NAME_PROPERTY, SPEC_NAME,
				"jackson.serialization-inclusion", "non_null",
				"jackson.mapper-features.sort-properties-alphabetically", "false",
				"jackson.deserialization-features.fail-on-null-for-primitives", "false",
				"jackson.date-time-features.write-dates-as-timestamps", "false");
	}

	private static Stream<Arguments> codecCases() {
		String validatedPath = CodecTestController.BASE_PATH + CodecTestController.VALIDATED_PATH;
		String fieldCleanedPath = CodecTestController.BASE_PATH + CodecTestController.FIELD_CLEANED_PATH;
		String plainPath = CodecTestController.BASE_PATH + CodecTestController.PLAIN_PATH;
		String primitivesPath = CodecTestController.BASE_PATH + CodecTestController.PRIMITIVES_PATH;
		String validExtension = String.format("{\"id\":\"%s\",\"@schemaLocation\":\"%s\",\"extension\":\"ok\"}",
				CodecTestController.ID, SCHEMA_LOCATION);
		return Stream.of(
				Arguments.of("Extensions valid for the @schemaLocation are accepted.",
						HttpRequest.POST(validatedPath, validExtension), HttpStatus.OK, validExtension),
				Arguments.of("Extensions invalid for the @schemaLocation are rejected.",
						HttpRequest.POST(validatedPath, String.format(
								"{\"id\":\"%s\",\"@schemaLocation\":\"%s\",\"extension\":42}",
								CodecTestController.ID, SCHEMA_LOCATION)),
						HttpStatus.BAD_REQUEST, null),
				Arguments.of("Extensions without a @schemaLocation are rejected.",
						HttpRequest.POST(validatedPath,
								String.format("{\"id\":\"%s\",\"extension\":\"ok\"}", CodecTestController.ID)),
						HttpStatus.BAD_REQUEST, null),
				Arguments.of("Without fields=, all fields are returned.",
						HttpRequest.GET(fieldCleanedPath), HttpStatus.OK,
						String.format("{\"id\":\"%s\",\"href\":\"%s\",\"name\":\"%s\",\"description\":\"%s\"}",
								CodecTestController.ID, CodecTestController.HREF, CodecTestController.NAME,
								CodecTestController.DESCRIPTION)),
				Arguments.of("With fields=, only the requested and the mandatory fields are returned.",
						HttpRequest.GET(fieldCleanedPath + "?fields=name"), HttpStatus.OK,
						String.format("{\"id\":\"%s\",\"href\":\"%s\",\"name\":\"%s\"}",
								CodecTestController.ID, CodecTestController.HREF, CodecTestController.NAME)),
				Arguments.of("Nulls are dropped, empty lists kept and property lists written as plain arrays.",
						HttpRequest.GET(plainPath), HttpStatus.OK,
						String.format("{\"id\":\"%s\",\"tags\":[],\"properties\":[{\"type\":\"Property\",\"value\":\"%s\"}]}",
								CodecTestController.ID, CodecTestController.PROPERTY_VALUE)),
				Arguments.of("Null for a primitive is read as its default.",
						HttpRequest.POST(primitivesPath,
								String.format("{\"id\":\"%s\",\"count\":null,\"enabled\":null}", CodecTestController.ID)),
						HttpStatus.OK,
						String.format("{\"id\":\"%s\",\"count\":0,\"enabled\":false}", CodecTestController.ID)));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("codecCases")
	void appliesMapperCustomisationsThroughTheCodec(String description, HttpRequest<?> request,
			HttpStatus expectedStatus, String expectedBody) {
		HttpResponse<String> response = exchange(request);

		assertEquals(expectedStatus, response.getStatus(), description);
		JsonNode body = EXPECTATION_MAPPER.readTree(response.body());
		if (expectedBody != null) {
			JsonNode expected = EXPECTATION_MAPPER.readTree(expectedBody);
			assertEquals(expected, body, description);
			// JsonNode equality ignores key order, so check declaration order (no alphabetical sorting) on its own
			assertEquals(List.copyOf(expected.propertyNames()), List.copyOf(body.propertyNames()), description);
		} else {
			assertTrue(body.get(MESSAGE_FIELD).asString().startsWith(SCHEMA_VALIDATION_MESSAGE_PREFIX), description);
		}
	}

	private static Stream<Arguments> mixinCases() {
		return Stream.of(
				Arguments.of(MapperKind.CONTEXT, PropertyListVO.class, ListVOMixin.class),
				Arguments.of(MapperKind.CONTEXT, RelationshipListVO.class, ListVOMixin.class),
				Arguments.of(MapperKind.CONTEXT, GeoPropertyListVO.class, ListVOMixin.class),
				Arguments.of(MapperKind.CONTEXT, AdditionalPropertyVO.class, AdditionalPropertyMixin.class),
				// the cache mapper writes the concrete class as type id, which the ListVOMixin's as=List would lose
				Arguments.of(MapperKind.CACHE, PropertyListVO.class, null),
				Arguments.of(MapperKind.CACHE, RelationshipListVO.class, null),
				Arguments.of(MapperKind.CACHE, GeoPropertyListVO.class, null),
				Arguments.of(MapperKind.CACHE, AdditionalPropertyVO.class, AdditionalPropertyDiscriminatorMixin.class));
	}

	@ParameterizedTest(name = "{0}: {1}")
	@MethodSource("mixinCases")
	void appliesMixinsPerMapper(MapperKind mapperKind, Class<?> targetClass, Class<?> expectedMixin) {
		ObjectMapper mapper = mapperKind == MapperKind.CACHE ? cacheMapper : objectMapper;
		assertEquals(expectedMixin, mapper.serializationConfig().findMixInClassFor(targetClass),
				"Each mapper should carry only its own mixin for the class.");
	}

	@ParameterizedTest
	@ValueSource(classes = {TmForumMappingModule.class, CodecTestFieldCleaningModule.class, NgsiLdMappingModule.class})
	void keepsApplicationModulesOffTheCacheMapper(Class<? extends JacksonModule> moduleClass) {
		assertFalse(cacheMapper.registeredModules().stream().map(JacksonModule::getClass).anyMatch(moduleClass::equals),
				"The cache mapper must not get the modules of the application mapper.");
	}

	private static Stream<Arguments> cachedEntities() {
		return Stream.of(
				Arguments.of("property", "\"status\":{\"type\":\"Property\",\"value\":\"active\"}"),
				Arguments.of("relationship", "\"rel\":{\"type\":\"Relationship\",\"object\":\"urn:ngsi-ld:rel:1\"}"),
				Arguments.of("multi-attribute relationship", "\"relList\":["
						+ "{\"type\":\"Relationship\",\"object\":\"urn:ngsi-ld:rel:1\",\"datasetId\":\"urn:ngsi-ld:dataset:1\"},"
						+ "{\"type\":\"Relationship\",\"object\":\"urn:ngsi-ld:rel:2\",\"datasetId\":\"urn:ngsi-ld:dataset:2\"}]"),
				Arguments.of("multi-attribute property", "\"color\":["
						+ "{\"type\":\"Property\",\"value\":\"red\",\"datasetId\":\"urn:ngsi-ld:dataset:1\"},"
						+ "{\"type\":\"Property\",\"value\":\"blue\",\"datasetId\":\"urn:ngsi-ld:dataset:2\"}]"),
				Arguments.of("null-valued sub-attribute",
						"\"status\":{\"type\":\"Property\",\"value\":\"active\","
								+ "\"reason\":{\"type\":\"Property\",\"value\":null}}"),
				Arguments.of("null member of a structured value",
						"\"sub\":{\"type\":\"Property\",\"value\":{\"name\":\"sub\",\"status\":null}}"));
	}

	// the cache mapper lacks ListVOMixin, NON_NULL and ValidatingDeserializer; this shows what the API writes is unchanged
	@ParameterizedTest(name = "{0}")
	@MethodSource("cachedEntities")
	void roundTripsEntityThroughTheCacheMapper(String description, String attributes) {
		EntityVO entityVO = objectMapper.readValue(String.format(CACHED_ENTITY_TEMPLATE, attributes), EntityVO.class);

		EntityVO cached = cacheMapper.readValue(cacheMapper.writeValueAsBytes(entityVO), EntityVO.class);

		assertFalse(entityVO.getAdditionalProperties().isEmpty(), "The entity should carry additional properties.");
		assertEquals(entityVO, cached, "The entity should survive the cache round trip unchanged.");
		assertEquals(attributeClasses(entityVO), attributeClasses(cached),
				"Every attribute should keep its VO class, which list equality alone would not show.");
		assertEquals(objectMapper.writeValueAsString(entityVO), objectMapper.writeValueAsString(cached),
				"The cached entity should be written by the API exactly like the original.");
	}

	private static Map<String, Class<?>> attributeClasses(EntityVO entityVO) {
		return entityVO.getAdditionalProperties().entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getClass()));
	}

	/** Which of the two mappers a case checks. */
	enum MapperKind {
		CONTEXT, CACHE
	}

	private HttpResponse<String> exchange(HttpRequest<?> request) {
		try {
			return httpClient.toBlocking().exchange(request, String.class);
		} catch (HttpClientResponseException e) {
			return HttpResponse.<String>status(e.getStatus())
					.body(e.getResponse().getBody(String.class).orElse(null));
		}
	}
}
