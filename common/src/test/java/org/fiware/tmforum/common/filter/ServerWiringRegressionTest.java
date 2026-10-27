package org.fiware.tmforum.common.filter;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.mapping.StubBrokerController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards server wiring that compiles unchanged on Micronaut 5 but could silently stop applying: the
 * {@code @Filter} server filters and their order, and the status mapping of the {@code ExceptionHandler}s.
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServerWiringRegressionTest implements TestPropertyProvider {

	public static final String SPEC_NAME = "ServerWiringRegressionTest";

	private static final String FORWARDED = "proto=https;host=api.example.org:443";
	private static final String EXPECTED_SELF_LINK =
			"<https://api.example.org/wiring-test/list?offset=0&limit=2>; rel=\"self\"";
	private static final String EXPECTED_NEXT_LINK =
			"<https://api.example.org/wiring-test/list?offset=2&limit=2>; rel=\"next\"";
	private static final String RESULT_COUNT_HEADER = "X-Result-Count";

	@Inject
	@Client("/")
	private HttpClient httpClient;

	@Override
	public Map<String, String> getProperties() {
		return Map.of(StubBrokerController.SPEC_NAME_PROPERTY, SPEC_NAME);
	}

	@Test
	void appliesForwardedForAndPaginationFilters() {
		HttpResponse<String> response = httpClient.toBlocking().exchange(
				HttpRequest.GET(WiringTestController.BASE_PATH + WiringTestController.LIST_PATH)
						.header(HttpHeaders.FORWARDED, FORWARDED),
				String.class);

		String link = response.header(HttpHeaders.LINK);
		// the forwarded host in the links proves ForwardedForFilter ran before PaginationFilter read its attribute
		assertTrue(link.contains(EXPECTED_SELF_LINK), "The self link should use the forwarded host, was: " + link);
		assertTrue(link.contains(EXPECTED_NEXT_LINK), "A full page should link the next one, was: " + link);
		assertEquals(String.valueOf(WiringTestController.LIST_ITEMS.size()), response.header(RESULT_COUNT_HEADER));
	}

	private static Stream<Arguments> exceptionStatuses() {
		return Stream.of(
				Arguments.of("tmforum-conflict", HttpStatus.CONFLICT),
				Arguments.of("tmforum-invalid-data", HttpStatus.BAD_REQUEST),
				Arguments.of("tmforum-invalid-relationship", HttpStatus.BAD_REQUEST),
				Arguments.of("tmforum-not-found", HttpStatus.NOT_FOUND),
				Arguments.of("tmforum-unknown", HttpStatus.INTERNAL_SERVER_ERROR),
				Arguments.of("deletion-not-found", HttpStatus.NOT_FOUND),
				Arguments.of("deletion-unknown", HttpStatus.INTERNAL_SERVER_ERROR),
				Arguments.of("query", HttpStatus.BAD_REQUEST),
				Arguments.of("schema-validation", HttpStatus.BAD_REQUEST),
				Arguments.of("http-client", HttpStatus.BAD_GATEWAY),
				Arguments.of("date-time-parse", HttpStatus.BAD_REQUEST),
				Arguments.of("unexpected", HttpStatus.INTERNAL_SERVER_ERROR));
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("exceptionStatuses")
	void mapsExceptionsToStatus(String exception, HttpStatus expectedStatus) {
		HttpClientResponseException thrown = assertThrows(
				HttpClientResponseException.class,
				() -> httpClient.toBlocking().exchange(
						HttpRequest.GET(WiringTestController.BASE_PATH + WiringTestController.THROW_PATH + exception),
						String.class));

		assertEquals(expectedStatus, thrown.getStatus());
		ErrorDetails errorDetails = thrown.getResponse().getBody(ErrorDetails.class).orElseThrow();
		assertEquals(expectedStatus.toString(), errorDetails.code(), "The handler should write the ErrorDetails body.");
	}

	@Test
	void coversEveryTestException() {
		Set<String> covered = exceptionStatuses().map(arguments -> (String) arguments.get()[0])
				.collect(Collectors.toSet());
		assertEquals(WiringTestController.EXCEPTIONS.keySet(), covered);
	}
}
