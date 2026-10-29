package org.fiware.tmforum.common.rest;

import io.github.wistefan.mapping.JavaObjectMapper;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.fiware.ngsi.model.EntityListVO;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.EventConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the HTTP error responses of the storage calls behind {@link AbstractApiController} and
 * {@link AbstractSubscriptionApiController}: status and {@link ErrorDetails} only, never exception types, so they
 * hold across the storage-neutral error refactoring. Runs with the append update; the subclass runs the replace one.
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ErrorResponseCharacterizationTest implements TestPropertyProvider {

	static final String CA_MESSAGE = "Request could not be answered due to an unexpected internal error.";
	static final String BAD_GATEWAY_MESSAGE = "Context broker is not reachable.";
	static final String DELETION_NOT_FOUND_MESSAGE = "The requested entity doesn't exist.";
	static final String DELETION_UNKNOWN_MESSAGE = "An unexpected error happened.";
	static final String LIST_MESSAGE = "An unexpected error happened - Was not able to list entities.";
	static final String CREATING = "creating";
	static final String UPDATING = "updating";

	private static final String ENTITIES = CharacterizationTestController.BASE_PATH
			+ CharacterizationTestController.ENTITIES_PATH;
	private static final String DIRECT = CharacterizationTestController.BASE_PATH
			+ CharacterizationTestController.DIRECT_PATH;
	private static final String HUB = CharacterizationSubscriptionController.BASE_PATH
			+ CharacterizationSubscriptionController.HUB_PATH;
	private static final String CALLBACK = CharacterizationSubscriptionController.BASE_PATH
			+ EventConstants.SUBSCRIPTION_CALLBACK_PATH;
	private static final String SUBSCRIBER = "http://subscriber.example.org/listener";
	private static final String NOTIFICATION = """
			{"id":"urn:ngsi-ld:Notification:%s","type":"Notification","subscriptionId":"%s",
			"notifiedAt":"2024-01-01T00:00:00Z","data":[{"id":"urn:ngsi-ld:characterization-entity:1",
			"type":"characterization-entity"}]}""";

	@Inject
	@Client("/")
	HttpClient httpClient;
	@Inject
	ScriptedBroker broker;
	@Inject
	JavaObjectMapper javaObjectMapper;

	/** The broker's answer to a call: an error status or no answer at all. */
	enum Failure {
		BAD_REQUEST(HttpStatus.BAD_REQUEST),
		NOT_FOUND(HttpStatus.NOT_FOUND),
		CONFLICT(HttpStatus.CONFLICT),
		INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
		NO_ANSWER(null);

		private final HttpStatus status;

		Failure(HttpStatus status) {
			this.status = status;
		}

		<T> Mono<T> mono() {
			return status == null ? ScriptedBroker.noAnswer() : ScriptedBroker.status(status);
		}

		String body() {
			return ScriptedBroker.body(status);
		}
	}

	/** The response the server is expected to answer with. */
	record Expected(HttpStatus status, String message) {
	}

	protected boolean replaceOnUpdate() {
		return false;
	}

	@Override
	public Map<String, String> getProperties() {
		return Map.of(
				ScriptedBroker.ENABLED_PROPERTY, "true",
				"general.replaceOnUpdate", String.valueOf(replaceOnUpdate()),
				"mapping.strictRelationships", "false",
				"micronaut.caches.entities.maximum-size", "1000",
				"micronaut.caches.subscriptions.maximum-size", "1000");
	}

	@BeforeEach
	void resetBroker() {
		broker.reset();
	}

	@AfterEach
	void assertOnlyScriptedCalls() {
		assertTrue(broker.unscriptedCalls().isEmpty(), "Unscripted broker calls: " + broker.unscriptedCalls());
	}

	static Expected caInternal() {
		return new Expected(HttpStatus.INTERNAL_SERVER_ERROR, CA_MESSAGE);
	}

	static Expected badGateway() {
		return new Expected(HttpStatus.BAD_GATEWAY, BAD_GATEWAY_MESSAGE);
	}

	static Expected listFailed() {
		return new Expected(HttpStatus.INTERNAL_SERVER_ERROR, LIST_MESSAGE);
	}

	static Expected deletionNotFound() {
		return new Expected(HttpStatus.NOT_FOUND, DELETION_NOT_FOUND_MESSAGE);
	}

	static Expected deletionUnknown() {
		return new Expected(HttpStatus.INTERNAL_SERVER_ERROR, DELETION_UNKNOWN_MESSAGE);
	}

	static Expected conflict(String action, Failure failure) {
		return new Expected(HttpStatus.CONFLICT, String.format(
				"At least one of the entities already exists - Conflict on %s the entity: %s", action, failure.body()));
	}

	static Expected invalidEntity(Failure failure) {
		return new Expected(HttpStatus.BAD_REQUEST, String.format(
				"The request contained invalid data - Did not receive a valid entity: %s.", failure.body()));
	}

	static Expected downstream(Failure failure) {
		return new Expected(HttpStatus.INTERNAL_SERVER_ERROR, String.format(
				"An unexpected error happened - Unspecified downstream error: %s", failure.body()));
	}

	static Stream<Arguments> rejectedWrites(String action) {
		return Stream.of(
				Arguments.of(Failure.BAD_REQUEST, invalidEntity(Failure.BAD_REQUEST)),
				Arguments.of(Failure.NOT_FOUND, downstream(Failure.NOT_FOUND)),
				Arguments.of(Failure.CONFLICT, conflict(action, Failure.CONFLICT)),
				Arguments.of(Failure.INTERNAL_SERVER_ERROR, downstream(Failure.INTERNAL_SERVER_ERROR)),
				Arguments.of(Failure.NO_ANSWER, badGateway()));
	}

	static Stream<Arguments> rejectedCreates() {
		return rejectedWrites(CREATING);
	}

	static Stream<Arguments> rejectedUpdates() {
		return rejectedWrites(UPDATING);
	}

	static Stream<Arguments> failedReads() {
		return Stream.of(
				Arguments.of(Failure.BAD_REQUEST, caInternal()),
				Arguments.of(Failure.CONFLICT, caInternal()),
				Arguments.of(Failure.INTERNAL_SERVER_ERROR, caInternal()),
				Arguments.of(Failure.NO_ANSWER, caInternal()));
	}

	static Stream<Arguments> failedDeletes() {
		return Stream.of(
				Arguments.of(Failure.BAD_REQUEST, deletionUnknown()),
				Arguments.of(Failure.NOT_FOUND, deletionNotFound()),
				Arguments.of(Failure.CONFLICT, deletionUnknown()),
				Arguments.of(Failure.INTERNAL_SERVER_ERROR, deletionUnknown()),
				Arguments.of(Failure.NO_ANSWER, deletionUnknown()));
	}

	static Stream<Arguments> failedRetrieves() {
		return Stream.concat(failedReads(), Stream.of(Arguments.of(Failure.NOT_FOUND,
				new Expected(HttpStatus.NOT_FOUND, "The requested object could not be found - No such entity exists."))));
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("rejectedCreates")
	void createFailsOnTheWrite(Failure failure, Expected expected) {
		broker.answer("createEntity", args -> failure.mono());

		assertResponse(expected, HttpRequest.POST(ENTITIES, ""));
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("failedReads")
	void createFailsOnTheReferenceValidation(Failure failure, Expected expected) {
		broker.answer("retrieveEntityById", args -> failure.mono());

		assertResponse(expected, HttpRequest.POST(ENTITIES + "?reference=" + newRelatedId(), ""));
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("rejectedCreates")
	void createFailsOnTheRelatedEntityWrite(Failure failure, Expected expected) {
		scriptRelatedUpdateFails();
		broker.answer("createEntity", args -> failure.mono());

		assertResponse(expected, HttpRequest.POST(ENTITIES + "?related=" + newRelatedId(), ""));
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("rejectedUpdates")
	void patchFailsOnTheWrite(Failure failure, Expected expected) {
		String id = newEntityId();
		scriptStored(id);
		scriptUpdateFails(failure);

		assertResponse(expected, HttpRequest.PATCH(ENTITIES + "/" + id, ""));
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("failedReads")
	void patchFailsOnTheReferenceValidation(Failure failure, Expected expected) {
		String id = newEntityId();
		EntityVO stored = javaObjectMapper.toEntityVO(new CharacterizationEntity(id));
		broker.answer("retrieveEntityById", args -> id.equals(args[0].toString())
				? Mono.just(HttpResponse.ok(stored)) : failure.mono());

		assertResponse(expected, HttpRequest.PATCH(ENTITIES + "/" + id + "?reference=" + newRelatedId(), ""));
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("rejectedUpdates")
	void patchFailsOnTheRelatedEntityWrite(Failure failure, Expected expected) {
		String id = newEntityId();
		scriptStored(id);
		scriptRelatedUpdateFails();
		broker.answer("createEntity", args -> failure.mono());

		assertResponse(expected, HttpRequest.PATCH(ENTITIES + "/" + id + "?related=" + newRelatedId(), ""));
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("failedDeletes")
	void deleteFails(Failure failure, Expected expected) {
		broker.answer("removeEntityById", args -> failure.mono());

		assertResponse(expected, HttpRequest.DELETE(ENTITIES + "/" + newEntityId()));
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Failure.class)
	void listFails(Failure failure) {
		broker.answer("queryEntities", args -> failure.mono());

		assertResponse(listFailed(), HttpRequest.GET(ENTITIES));
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("failedRetrieves")
	void retrieveFails(Failure failure, Expected expected) {
		broker.answer("retrieveEntityById", args -> failure.mono());

		assertResponse(expected, HttpRequest.GET(ENTITIES + "/" + newEntityId()));
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = Failure.class, names = {"INTERNAL_SERVER_ERROR", "NO_ANSWER"})
	void listFailsOnTheRelationshipResolution(Failure failure) {
		EntityVO stored = storedWithRelated(newEntityId());
		EntityListVO entities = new EntityListVO();
		entities.add(stored);
		broker.answer("queryEntities", args -> Mono.just(HttpResponse.ok(entities)));
		broker.answer("retrieveEntityById", args -> failure.mono());

		assertResponse(listFailed(), HttpRequest.GET(ENTITIES));
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = Failure.class, names = {"INTERNAL_SERVER_ERROR", "NO_ANSWER"})
	void retrieveFailsOnTheRelationshipResolution(Failure failure) {
		String id = newEntityId();
		EntityVO stored = storedWithRelated(id);
		broker.answer("retrieveEntityById", args -> id.equals(args[0].toString())
				? Mono.just(HttpResponse.ok(stored)) : failure.mono());

		assertResponse(caInternal(), HttpRequest.GET(ENTITIES + "/" + id));
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("rejectedWritesUncaught")
	void directUpdateFailsUncaught(Failure failure, Expected expected) {
		scriptUpdateFails(failure);

		assertResponse(expected, HttpRequest.PATCH(DIRECT + "/" + newEntityId(), ""));
	}

	static Stream<Arguments> rejectedWritesUncaught() {
		return Stream.of(Failure.values()).map(failure -> Arguments.of(failure, badGateway()));
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Failure.class)
	void registerFailsOnTheDuplicateCheck(Failure failure) {
		broker.answer("queryEntities", args -> failure.mono());

		assertResponse(listFailed(), HttpRequest.POST(HUB + "?callback=" + SUBSCRIBER, ""));
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("rejectedWritesUncaught")
	void registerFailsOnTheSubscriptionCreation(Failure failure, Expected expected) {
		broker.answer("queryEntities", args -> Mono.just(HttpResponse.ok(new EntityListVO())));
		broker.answer("createSubscription", args -> failure.mono());

		assertResponse(expected, HttpRequest.POST(HUB + "?callback=" + SUBSCRIBER, ""));
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("failedReads")
	void unregisterFailsOnTheSubscriptionLookup(Failure failure, Expected expected) {
		broker.answer("retrieveEntityById", args -> failure.mono());
		scriptEntityDeletionAssembled();

		assertResponse(expected, HttpRequest.DELETE(HUB + "/" + newSubscriptionId()));
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("failedDeletes")
	void unregisterFailsOnTheSubscriptionRemoval(Failure failure, Expected expected) {
		String id = newSubscriptionId();
		scriptStoredSubscription(id);
		broker.answer("removeSubscription", args -> failure.mono());
		scriptEntityDeletionAssembled();

		assertResponse(expected, HttpRequest.DELETE(HUB + "/" + id));
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("failedDeletes")
	void unregisterFailsOnTheEntityDeletion(Failure failure, Expected expected) {
		String id = newSubscriptionId();
		scriptStoredSubscription(id);
		broker.answer("removeSubscription", args -> Mono.just(HttpResponse.noContent()));
		broker.answer("removeEntityById", args -> failure.mono());

		assertResponse(expected, HttpRequest.DELETE(HUB + "/" + id));
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("failedReads")
	void callbackFailsOnTheSubscriptionLookup(Failure failure, Expected expected) {
		broker.answer("retrieveSubscriptionById", args -> failure.mono());

		assertResponse(expected, callback());
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = Failure.class, names = "NOT_FOUND")
	void callbackForAnUnknownSubscriptionIsIgnored(Failure failure) {
		broker.answer("retrieveSubscriptionById", args -> failure.mono());

		HttpResponse<String> response = httpClient.toBlocking().exchange(callback(), String.class);

		assertEquals(HttpStatus.NO_CONTENT, response.getStatus(), "An unknown subscription should be ignored.");
	}

	/** Scripts the update of an existing entity failing on its write. */
	protected void scriptUpdateFails(Failure failure) {
		broker.answer("updateEntity", args -> failure.mono());
	}

	/** Scripts every update failing as for an absent entity, so the related-entity handling falls back to create. */
	protected void scriptRelatedUpdateFails() {
		broker.answer("updateEntity", args -> Failure.NOT_FOUND.mono());
	}

	// the subscription controller builds the entity deletion before the subscription removal has run
	private void scriptEntityDeletionAssembled() {
		broker.answer("removeEntityById", args -> Mono.just(HttpResponse.noContent()));
	}

	protected void scriptStored(String id) {
		EntityVO stored = javaObjectMapper.toEntityVO(new CharacterizationEntity(id));
		broker.answer("retrieveEntityById", args -> Mono.just(HttpResponse.ok(stored)));
	}

	protected void assertResponse(Expected expected, HttpRequest<?> request) {
		HttpClientResponseException exception = assertThrows(HttpClientResponseException.class,
				() -> httpClient.toBlocking().exchange(request, Argument.of(String.class),
						Argument.of(ErrorDetails.class)));

		ErrorDetails details = exception.getResponse().getBody(ErrorDetails.class).orElseThrow();
		assertEquals(expected.status(), exception.getStatus(), "Unexpected status.");
		assertEquals(expected.status().toString(), details.code(), "Unexpected error code.");
		assertEquals(expected.status().getReason(), details.reason(), "Unexpected error reason.");
		assertEquals(expected.message(), details.message(), "Unexpected error message.");
	}

	protected static String newEntityId() {
		return "urn:ngsi-ld:" + CharacterizationEntity.TYPE + ":" + UUID.randomUUID();
	}

	private static String newRelatedId() {
		return "urn:ngsi-ld:" + CharacterizationRelated.TYPE + ":" + UUID.randomUUID();
	}

	private static String newSubscriptionId() {
		return "urn:ngsi-ld:" + TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION + ":" + UUID.randomUUID();
	}

	private EntityVO storedWithRelated(String id) {
		CharacterizationEntity entity = new CharacterizationEntity(id);
		entity.setRelated(List.of(new CharacterizationRelated(newRelatedId())));
		return javaObjectMapper.toEntityVO(entity);
	}

	private void scriptStoredSubscription(String id) {
		TMForumSubscription subscription = new TMForumSubscription(id);
		subscription.setCallback(URI.create(SUBSCRIBER));
		subscription.setSubscription(new Subscription(UUID.randomUUID().toString()));
		EntityVO stored = javaObjectMapper.toEntityVO(subscription);
		broker.answer("retrieveEntityById", args -> Mono.just(HttpResponse.ok(stored)));
	}

	private static HttpRequest<String> callback() {
		String subscriptionId = "urn:ngsi-ld:Subscription:" + UUID.randomUUID();
		return HttpRequest.POST(CALLBACK, String.format(NOTIFICATION, UUID.randomUUID(), subscriptionId))
				.contentType(MediaType.APPLICATION_JSON_TYPE);
	}
}
