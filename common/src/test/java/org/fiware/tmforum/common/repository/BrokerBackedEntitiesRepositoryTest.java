package org.fiware.tmforum.common.repository;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.storage.StorageErrorReason;
import org.fiware.tmforum.storage.StorageException;
import org.fiware.tmforum.storage.StorageOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the read-path contract of {@link BrokerBackedEntitiesRepository#getEntities(List)}: every id yields an entity,
 * a missing target an untyped stub, and any other broker error fails the read.
 */
class BrokerBackedEntitiesRepositoryTest {

	private static final URI CATEGORY_A = URI.create("urn:ngsi-ld:category:a");
	private static final URI CATEGORY_B = URI.create("urn:ngsi-ld:category:b");
	private static final URI CATEGORY_C = URI.create("urn:ngsi-ld:category:c");
	private static final URI MISSING = URI.create("urn:ngsi-ld:category:missing");
	private static final URI REJECTED = URI.create("urn:ngsi-ld:category:rejected");

	private EntitiesApiClient entitiesApi;
	private BrokerBackedEntitiesRepository repository;

	@BeforeEach
	public void setUp() {
		entitiesApi = mock(EntitiesApiClient.class);
		repository = new BrokerBackedEntitiesRepository(new GeneralProperties(), entitiesApi);
		Stream.of(CATEGORY_A, CATEGORY_B, CATEGORY_C).forEach(id -> answer(id, Mono.just(HttpResponse.ok(entity(id)))));
		answer(MISSING, Mono.error(clientError(HttpStatus.NOT_FOUND)));
		answer(REJECTED, Mono.error(clientError(HttpStatus.BAD_REQUEST)));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("getEntitiesCases")
	public void getEntities(String description, List<URI> requested, List<URI> expectedFound) {
		List<EntityVO> entities = repository.getEntities(requested).block();

		assertEquals(requested, entities.stream().map(EntityVO::getId).toList(), description);
		// only fetched entities carry a type, the stubs of missing ones do not
		assertEquals(expectedFound, entities.stream().filter(e -> e.getType() != null).map(EntityVO::getId).toList(),
				description);
	}

	private static Stream<Arguments> getEntitiesCases() {
		return Stream.of(
				Arguments.of("empty input", List.of(), List.of()),
				Arguments.of("all found", List.of(CATEGORY_A, CATEGORY_B, CATEGORY_C),
						List.of(CATEGORY_A, CATEGORY_B, CATEGORY_C)),
				Arguments.of("one missing", List.of(CATEGORY_A, MISSING, CATEGORY_C), List.of(CATEGORY_A, CATEGORY_C)),
				Arguments.of("all missing", List.of(MISSING), List.of()));
	}

	@Test
	public void getEntities_brokerErrorPropagates() {
		Mono<List<EntityVO>> entities = repository.getEntities(List.of(CATEGORY_A, REJECTED));

		StorageException exception = assertThrows(StorageException.class, entities::block);

		assertEquals(StorageOperation.READ, exception.operation());
		assertEquals(StorageErrorReason.UNKNOWN, exception.reason());
	}

	private void answer(URI id, Mono<HttpResponse<EntityVO>> response) {
		when(entitiesApi.retrieveEntityById(eq(id), any(), any(), any(), any(), any())).thenReturn(response);
	}

	private static EntityVO entity(URI id) {
		return new EntityVO().id(id).type("category");
	}

	private static HttpClientResponseException clientError(HttpStatus status) {
		return new HttpClientResponseException(status.getReason(), HttpResponse.status(status));
	}
}
