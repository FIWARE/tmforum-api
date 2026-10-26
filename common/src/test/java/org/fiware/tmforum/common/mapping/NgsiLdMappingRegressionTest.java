package org.fiware.tmforum.common.mapping;

import io.github.wistefan.mapping.EntityVOMapper;
import io.github.wistefan.mapping.JavaObjectMapper;
import io.micronaut.core.io.socket.SocketUtils;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.ProblemDetailsVO;
import org.fiware.tmforum.common.querying.MyEntityPojo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Guards the Micronaut 5 build of ngsi-ld-java-mapping: its beans load, domain objects map to entities and back, and
 * the declarative {@link EntitiesApiClient} round-trips entities and decodes NGSI-LD error bodies.
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NgsiLdMappingRegressionTest implements TestPropertyProvider {

	public static final String SPEC_NAME = "NgsiLdMappingRegressionTest";

	private static final String ENTITY_ID_PREFIX = "urn:ngsi-ld:my-entity-pojo:";
	private static final String UNKNOWN_ENTITY_ID = ENTITY_ID_PREFIX + "unknown";
	private static final String NGSI_PATH = "ngsi-ld/v1";
	private static final String LD_JSON = "application/ld+json";

	@Inject
	private EntityVOMapper entityVOMapper;
	@Inject
	private JavaObjectMapper javaObjectMapper;
	@Inject
	private EntitiesApiClient entitiesApiClient;

	@Override
	public Map<String, String> getProperties() {
		int port = SocketUtils.findAvailableTcpPort();
		return Map.of(
				StubBrokerController.SPEC_NAME_PROPERTY, SPEC_NAME,
				"micronaut.server.port", String.valueOf(port),
				"micronaut.http.services.ngsi.url", "http://localhost:" + port,
				"micronaut.http.services.ngsi.path", NGSI_PATH,
				"micronaut.codec.json.additional-types", LD_JSON,
				"jackson.serialization-inclusion", "non_null");
	}

	@ParameterizedTest
	@CsvSource({"plain, active", "unicode, with spaces and ümlauts"})
	void mapsDomainObjectToEntityAndBack(String idSuffix, String status) {
		MyEntityPojo pojo = pojo(idSuffix, status);

		EntityVO entityVO = javaObjectMapper.toEntityVO(pojo);
		MyEntityPojo mappedBack = entityVOMapper.fromEntityVO(entityVO, MyEntityPojo.class).block();

		assertEquals(pojo.getId(), entityVO.getId(), "The entity should keep the id.");
		assertEquals(pojo.getType(), entityVO.getType(), "The entity should keep the type.");
		assertEquals(status, mappedBack.getStatus(), "The property should survive the round trip.");
	}

	@ParameterizedTest
	@CsvSource({"plain, active", "unicode, with spaces and ümlauts"})
	void roundTripsEntityThroughTheClient(String idSuffix, String status) {
		MyEntityPojo pojo = pojo(idSuffix, status);
		EntityVO entityVO = javaObjectMapper.toEntityVO(pojo);

		HttpResponse<Object> createResponse = entitiesApiClient.createEntity(entityVO, null).block();
		EntityVO retrieved = entitiesApiClient
				.retrieveEntityById(entityVO.getId(), null, null, null, null, null)
				.block()
				.body();
		MyEntityPojo mappedBack = entityVOMapper.fromEntityVO(retrieved, MyEntityPojo.class).block();

		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The entity should have been created.");
		assertEquals(entityVO.getId(), retrieved.getId(), "The retrieved entity should be the created one.");
		assertEquals(status, mappedBack.getStatus(), "The property should survive the HTTP round trip.");
	}

	@Test
	void decodesNgsiLdErrorBody() {
		HttpClientResponseException exception = assertThrows(HttpClientResponseException.class,
				() -> entitiesApiClient
						.retrieveEntityById(URI.create(UNKNOWN_ENTITY_ID), null, null, null, null, null)
						.block());

		ProblemDetailsVO problemDetails = exception.getResponse().getBody(ProblemDetailsVO.class).orElseThrow();
		assertEquals(HttpStatus.NOT_FOUND, exception.getStatus(), "Unknown entities should be reported as not found.");
		assertEquals(StubBrokerController.NOT_FOUND_TYPE, problemDetails.getType(), "The error type should be decoded.");
		assertEquals(StubBrokerController.NOT_FOUND_TITLE, problemDetails.getTitle(), "The error title should be decoded.");
		assertEquals(UNKNOWN_ENTITY_ID, problemDetails.getDetail(), "The error detail should be decoded.");
	}

	private static MyEntityPojo pojo(String idSuffix, String status) {
		MyEntityPojo pojo = new MyEntityPojo(ENTITY_ID_PREFIX + idSuffix);
		pojo.setStatus(status);
		return pojo;
	}
}
