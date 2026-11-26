package org.fiware.tmforum.accesspolicy;

import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.json.JsonMapper;
import org.fiware.tmforum.accesspolicy.AccessDecision.Source;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests that every PDP failure becomes a tagged decision instead of an error, and that a verdict is passed on as is.
 */
class PolicyDecisionPointTest {

	private static final String POLICY_UID = "urn:ngsi-ld:policy:offering-visibility";
	private static final String PDP_URL = "http://odrl-pap.example.org:8080";
	private static final String PDP_PATH = "/validate";
	private static final Duration PDP_TIMEOUT = Duration.ofMillis(250);
	private static final ValidationRequest REQUEST = new ValidationRequest(Map.of("uid", POLICY_UID), null);
	private static final JsonMapper JSON_MAPPER = JsonMapper.createDefault();

	private final HttpClient httpClient = mock(HttpClient.class);

	private PolicyDecisionPoint pdp(String url, boolean failOpen) {
		return pdp(url, PDP_PATH, failOpen);
	}

	private PolicyDecisionPoint pdp(String url, String path, boolean failOpen) {
		AccessPolicyProperties properties = new AccessPolicyProperties();
		properties.setFailOpen(failOpen);
		properties.getPdp().setUrl(url);
		properties.getPdp().setPath(path);
		properties.getPdp().setTimeout(PDP_TIMEOUT);
		return new PolicyDecisionPoint(properties, httpClient, JSON_MAPPER);
	}

	private void answer(Supplier<Publisher<HttpResponse<String>>> answer) {
		when(httpClient.exchange(any(HttpRequest.class), eq(String.class))).thenAnswer(invocation -> answer.get());
	}

	private static Stream<Arguments> failureModes() {
		Stream<Arguments> modes = Stream.of(
				Arguments.of("timeout", (Supplier<Publisher<HttpResponse<String>>>) Mono::never,
						"PDP timed out after " + PDP_TIMEOUT),
				Arguments.of("connect error",
						(Supplier<Publisher<HttpResponse<String>>>) () -> Mono.error(
								new HttpClientException("Connect Error: Connection refused")),
						"PDP unreachable: " + HttpClientException.class.getName()),
				Arguments.of("client throws synchronously",
						(Supplier<Publisher<HttpResponse<String>>>) () -> {
							throw new IllegalStateException("client closed");
						},
						"PDP unreachable: " + IllegalStateException.class.getName()),
				Arguments.of("500", errorStatus(HttpStatus.INTERNAL_SERVER_ERROR), "PDP answered 500"),
				Arguments.of("404", errorStatus(HttpStatus.NOT_FOUND), "PDP answered 404"),
				Arguments.of("204 without body", status(HttpStatus.NO_CONTENT, null), "PDP answered 204"),
				Arguments.of("201 with a verdict", status(HttpStatus.CREATED, "{\"permitted\":true}"),
						"PDP answered 201"),
				Arguments.of("completes empty", (Supplier<Publisher<HttpResponse<String>>>) Mono::empty,
						"PDP answered nothing"),
				Arguments.of("200 without body", status(HttpStatus.OK, null), "PDP answer unparsable"),
				Arguments.of("invalid JSON", status(HttpStatus.OK, "{\"permitted\":"), "PDP answer unparsable"),
				Arguments.of("empty object", status(HttpStatus.OK, "{}"), "PDP answer unparsable"),
				Arguments.of("string permitted", status(HttpStatus.OK, "{\"permitted\":\"yes\"}"),
						"PDP answer unparsable"),
				Arguments.of("boolean-like string permitted", status(HttpStatus.OK, "{\"permitted\":\"true\"}"),
						"PDP answer unparsable"),
				Arguments.of("array", status(HttpStatus.OK, "[true]"), "PDP answer unparsable"));
		return modes.flatMap(mode -> Stream.of(false, true).map(failOpen -> {
			Object[] values = mode.get();
			return Arguments.of(values[0], values[1], values[2], failOpen);
		}));
	}

	private static Supplier<Publisher<HttpResponse<String>>> status(HttpStatus status, String body) {
		return () -> Mono.just(body == null
				? HttpResponse.<String>status(status)
				: HttpResponse.<String>status(status).body(body));
	}

	private static Supplier<Publisher<HttpResponse<String>>> errorStatus(HttpStatus status) {
		return () -> Mono.error(new HttpClientResponseException(status.getReason(), HttpResponse.status(status)));
	}

	@DisplayName("A failed PDP call completes with a PDP_FAILURE decision that permits exactly under fail-open.")
	@ParameterizedTest(name = "{0}, fail-open {3}")
	@MethodSource("failureModes")
	void failureBecomesTaggedDecision(String name, Supplier<Publisher<HttpResponse<String>>> answer, String reason,
			boolean failOpen) {
		answer(answer);
		PolicyDecisionPoint pdp = pdp(PDP_URL, failOpen);

		StepVerifier.withVirtualTime(() -> pdp.decide(POLICY_UID, REQUEST))
				.thenAwait(PDP_TIMEOUT)
				.expectNext(new AccessDecision(failOpen, reason, POLICY_UID, Source.PDP_FAILURE))
				.verifyComplete();
	}

	private static Stream<Arguments> verdicts() {
		return Stream.of(
				Arguments.of("{\"permitted\":true,\"reason\":\"member of the catalog's party\"}", true,
						"member of the catalog's party"),
				Arguments.of("{\"permitted\":false,\"reason\":\"no matching credential\"}", false,
						"no matching credential"),
				Arguments.of("{\"permitted\":false}", false, null),
				Arguments.of("{\"permitted\":true,\"reason\":42}", true, null));
	}

	@DisplayName("A 200 answer with a boolean permitted is the policy's verdict, whatever fail-open says.")
	@ParameterizedTest(name = "{0}")
	@MethodSource("verdicts")
	void verdictIsPassedOn(String body, boolean permitted, String reason) {
		answer(status(HttpStatus.OK, body));

		StepVerifier.create(pdp(PDP_URL, !permitted).decide(POLICY_UID, REQUEST))
				.expectNext(new AccessDecision(permitted, reason, POLICY_UID, Source.POLICY))
				.verifyComplete();
	}

	private static Stream<Arguments> endpoints() {
		return Stream.of(
				Arguments.of(PDP_URL, PDP_URL + PDP_PATH),
				Arguments.of(PDP_URL + "/", PDP_URL + PDP_PATH),
				Arguments.of(PDP_URL + "/odrl", PDP_URL + "/odrl" + PDP_PATH),
				Arguments.of(null, PDP_PATH),
				Arguments.of(" ", PDP_PATH));
	}

	@DisplayName("The request is a JSON POST to pdp.url plus path, or to the bare path the pdp service resolves.")
	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("endpoints")
	void postsJsonToTheEndpoint(String url, String expectedUri) {
		answer(status(HttpStatus.OK, "{\"permitted\":true}"));

		StepVerifier.create(pdp(url, false).decide(POLICY_UID, REQUEST))
				.expectNextCount(1)
				.verifyComplete();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<HttpRequest<ValidationRequest>> sent = ArgumentCaptor.forClass(HttpRequest.class);
		verify(httpClient).exchange(sent.capture(), eq(String.class));
		assertEquals(HttpMethod.POST, sent.getValue().getMethod(), "The PDP is asked with a POST.");
		assertEquals(Optional.of(MediaType.APPLICATION_JSON_TYPE), sent.getValue().getContentType(),
				"The request is sent as JSON.");
		assertEquals(expectedUri, sent.getValue().getUri().toString(), "The request goes to the configured endpoint.");
		assertSame(REQUEST, sent.getValue().getBody().orElseThrow(), "The validation request is the body.");
	}

	@DisplayName("A path that is no URI fails the client's creation instead of every call.")
	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"/val idate", "/{policy}"})
	void malformedPathFailsCreation(String path) {
		assertThrows(IllegalArgumentException.class, () -> pdp(null, path, false),
				"A malformed relative path must not wait for the first call.");
	}
}
