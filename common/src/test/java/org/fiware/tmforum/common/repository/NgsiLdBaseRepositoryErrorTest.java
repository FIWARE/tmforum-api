package org.fiware.tmforum.common.repository;

import io.github.wistefan.mapping.JavaObjectMapper;
import io.github.wistefan.mapping.NgsiLdWriteOperation;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.mapping.NGSIMapper;
import org.fiware.tmforum.storage.StorageErrorReason;
import org.fiware.tmforum.storage.StorageException;
import org.fiware.tmforum.storage.StorageOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The adapter translates every broker failure of the create, append-update, delete and list paths into a
 * {@link StorageException} carrying the failed operation and the reason the call sites render.
 */
class NgsiLdBaseRepositoryErrorTest {

	private static final String ENTITY_ID = "urn:ngsi-ld:test:error";
	private static final String RESPONSE_BODY = "{\"title\":\"broker problem\"}";
	private static final String NO_ANSWER = "Connect Error: connection refused";
	private static final String LIST_FAILED = "Was not able to list entities.";

	private EntitiesApiClient entitiesApi;
	private TmForumRepository repository;

	@BeforeEach
	public void setUp() {
		entitiesApi = mock(EntitiesApiClient.class);
		JavaObjectMapper javaObjectMapper = mock(JavaObjectMapper.class);
		EntityVO entityVO = new EntityVO().id(URI.create(ENTITY_ID)).type("test");
		when(javaObjectMapper.toEntityVO(any())).thenReturn(entityVO);
		when(javaObjectMapper.toEntityVO(any(), any(NgsiLdWriteOperation.class))).thenReturn(entityVO);
		NGSIMapper ngsiMapper = new NGSIMapper() {
		};
		repository = new TmForumRepository(new GeneralProperties(), entitiesApi, null, null, ngsiMapper,
				javaObjectMapper);
	}

	@ParameterizedTest(name = "{0} failing with {1}")
	@MethodSource("writeFailures")
	public void aFailedWriteCarriesTheBrokerAnswer(StorageOperation operation, HttpClientException failure,
			StorageErrorReason expectedReason, String expectedMessage) {
		Mono<Void> write = switch (operation) {
			case CREATE -> {
				when(entitiesApi.createEntity(any(), any())).thenReturn(Mono.error(failure));
				yield repository.createDomainEntity(new Object());
			}
			case UPDATE -> {
				when(entitiesApi.updateEntity(any(), any(), any(), any())).thenReturn(Mono.error(failure));
				yield repository.updateDomainEntity(ENTITY_ID, new Object());
			}
			default -> throw new IllegalArgumentException("Not a write: " + operation);
		};

		assertStorageException(operation, expectedReason, expectedMessage, write::block);
	}

	private static Stream<Arguments> writeFailures() {
		return Stream.of(StorageOperation.CREATE, StorageOperation.UPDATE).flatMap(operation -> Stream.of(
				Arguments.of(operation, answered(HttpStatus.BAD_REQUEST), StorageErrorReason.INVALID_DATA,
						RESPONSE_BODY),
				Arguments.of(operation, answered(HttpStatus.NOT_FOUND), StorageErrorReason.NOT_FOUND, RESPONSE_BODY),
				Arguments.of(operation, answered(HttpStatus.CONFLICT), StorageErrorReason.CONFLICT, RESPONSE_BODY),
				Arguments.of(operation, answered(HttpStatus.INTERNAL_SERVER_ERROR), StorageErrorReason.UNKNOWN,
						RESPONSE_BODY),
				Arguments.of(operation, answeredWithoutBody(HttpStatus.CONFLICT), StorageErrorReason.CONFLICT,
						HttpStatus.CONFLICT.getReason()),
				Arguments.of(operation, new HttpClientException(NO_ANSWER), StorageErrorReason.UNAVAILABLE,
						NO_ANSWER)));
	}

	@ParameterizedTest(name = "delete failing with {0}")
	@MethodSource("deleteFailures")
	public void aFailedDeleteIsTranslated(HttpClientException failure, StorageErrorReason expectedReason,
			String expectedMessage) {
		when(entitiesApi.removeEntityById(any(), any(), any())).thenReturn(Mono.error(failure));

		assertStorageException(StorageOperation.DELETE, expectedReason, expectedMessage,
				() -> repository.deleteDomainEntity(URI.create(ENTITY_ID)).block());
	}

	private static Stream<Arguments> deleteFailures() {
		String notFound = String.format("Was not able to delete %s, since it does not exist.", ENTITY_ID);
		String failed = String.format("Was not able to delete %s.", ENTITY_ID);
		return Stream.of(
				Arguments.of(answered(HttpStatus.NOT_FOUND), StorageErrorReason.NOT_FOUND, notFound),
				Arguments.of(answered(HttpStatus.BAD_REQUEST), StorageErrorReason.UNKNOWN, failed),
				Arguments.of(answered(HttpStatus.INTERNAL_SERVER_ERROR), StorageErrorReason.UNKNOWN, failed),
				Arguments.of(new HttpClientException(NO_ANSWER), StorageErrorReason.UNAVAILABLE, failed));
	}

	@ParameterizedTest(name = "{0} failing with {1}")
	@MethodSource("listFailures")
	public void aFailedListIsTranslated(String listMethod, HttpClientException failure,
			StorageErrorReason expectedReason) {
		when(entitiesApi.queryEntities(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
				any(), any(), any(), any(), any(), any())).thenReturn(Mono.error(failure));
		Function<TmForumRepository, Mono<?>> list = listMethod.equals("findEntities")
				? r -> r.findEntities(0, 10, Object.class, null, null, "test", null)
				: r -> r.findEntitiesPolymorphic(0, 10, "test", null, null, null, type -> Object.class);

		assertStorageException(StorageOperation.LIST, expectedReason, LIST_FAILED,
				() -> list.apply(repository).block());
	}

	private static Stream<Arguments> listFailures() {
		return Stream.of("findEntities", "findEntitiesPolymorphic").flatMap(method -> Stream.of(
				Arguments.of(method, answered(HttpStatus.BAD_REQUEST), StorageErrorReason.UNKNOWN),
				Arguments.of(method, answered(HttpStatus.INTERNAL_SERVER_ERROR), StorageErrorReason.UNKNOWN),
				Arguments.of(method, new HttpClientException(NO_ANSWER), StorageErrorReason.UNAVAILABLE)));
	}

	private static void assertStorageException(StorageOperation operation, StorageErrorReason reason, String message,
			Runnable call) {
		StorageException exception = assertThrows(StorageException.class, call::run);
		assertEquals(operation, exception.operation());
		assertEquals(reason, exception.reason());
		assertEquals(message, exception.getMessage());
	}

	private static HttpClientResponseException answered(HttpStatus status) {
		return new HttpClientResponseException(status.getReason(), HttpResponse.status(status).body(RESPONSE_BODY));
	}

	private static HttpClientResponseException answeredWithoutBody(HttpStatus status) {
		return new HttpClientResponseException(status.getReason(), HttpResponse.status(status));
	}
}
