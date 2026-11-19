package org.fiware.tmforum.common.rest;

import io.micronaut.context.ApplicationContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.runtime.server.EmbeddedServer;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.notification.EventConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pins that {@code POST /callback} is routed to the controller on the broker and unmatched (plain 404) on PostgreSQL,
 * where the NGSI-LD notification path is not wired.
 */
class CallbackRouteTest {

	private static final String CALLBACK = CharacterizationSubscriptionController.BASE_PATH
			+ EventConstants.SUBSCRIPTION_CALLBACK_PATH;
	private static final String JSON_UTF8 = "application/json;charset=utf-8";
	private static final String NOTIFICATION = """
			{"id":"urn:ngsi-ld:Notification:%s","type":"Notification","subscriptionId":"urn:ngsi-ld:Subscription:%s",
			"notifiedAt":"2024-01-01T00:00:00Z","data":[{"id":"urn:ngsi-ld:characterization-entity:1",
			"type":"characterization-entity"}]}""";
	private static final String NOT_JSON = "this is not json";
	private static final Map<String, Object> PROPERTIES = Map.of(
			ScriptedBroker.ENABLED_PROPERTY, "true",
			"micronaut.server.port", "-1",
			"mapping.strictRelationships", "false",
			"micronaut.caches.entities.maximum-size", "1000",
			"micronaut.caches.subscriptions.maximum-size", "1000");

	@Test
	void callbackIsRoutedOnTheBroker() {
		try (EmbeddedServer server = ApplicationContext.run(EmbeddedServer.class, PROPERTIES);
			 HttpClient client = HttpClient.create(server.getURL())) {
			server.getApplicationContext().getBean(ScriptedBroker.class)
					.answer("retrieveSubscriptionById", args -> ScriptedBroker.status(HttpStatus.NOT_FOUND));

			HttpResponse<String> response = client.toBlocking()
					.exchange(post(notification(), JSON_UTF8), String.class);

			assertEquals(HttpStatus.NO_CONTENT, response.getStatus(),
					"The callback should reach the controller and ignore the unknown subscription.");
		}
	}

	static Stream<Arguments> postgresqlRequests() {
		return Stream.of(
				Arguments.of("valid notification", notification(), JSON_UTF8),
				Arguments.of("non-JSON payload", NOT_JSON, JSON_UTF8),
				Arguments.of("empty body", "", JSON_UTF8),
				Arguments.of("wrong content type", notification(), MediaType.TEXT_PLAIN));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("postgresqlRequests")
	void callbackIsUnmatchedOnPostgresql(String description, String body, String contentType) {
		try (EmbeddedServer server = ApplicationContext.run(EmbeddedServer.class, PROPERTIES,
				CommonConstants.POSTGRESQL_ENVIRONMENT);
			 HttpClient client = HttpClient.create(server.getURL())) {
			HttpClientResponseException exception = assertThrows(HttpClientResponseException.class,
					() -> client.toBlocking().exchange(post(body, contentType), String.class));

			assertEquals(HttpStatus.NOT_FOUND, exception.getStatus(),
					"The callback route should not exist on PostgreSQL.");
		}
	}

	private static String notification() {
		return String.format(NOTIFICATION, UUID.randomUUID(), UUID.randomUUID());
	}

	private static HttpRequest<String> post(String body, String contentType) {
		return HttpRequest.POST(CALLBACK, body).contentType(contentType);
	}
}
