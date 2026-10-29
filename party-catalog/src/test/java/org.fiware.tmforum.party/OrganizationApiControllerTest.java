package org.fiware.tmforum.party;

import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.party.domain.organization.Organization;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the error responses of the organization delete, which calls the repository without the call-site mapping.
 */
@MicronautTest(packages = {"org.fiware.tmforum.party"}, environments = "in-memory",
		contextBuilder = FirstMatchConfigContextBuilder.class)
class OrganizationApiControllerTest {

	private static final String ORGANIZATION_PATH = "/organization/";

	@Inject
	@Client("/")
	HttpClient httpClient;
	@Inject
	EntitiesApiClient entitiesApiClient;

	@MockBean(EntitiesApiClient.class)
	EntitiesApiClient entitiesApiClient() {
		return mock(EntitiesApiClient.class);
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@CsvSource({
			"NOT_FOUND, NOT_FOUND, The requested entity doesn't exist.",
			"INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR, An unexpected error happened."})
	void deleteFails(HttpStatus brokerStatus, HttpStatus expectedStatus, String expectedMessage) {
		when(entitiesApiClient.removeEntityById(any(), any(), any())).thenReturn(Mono.error(
				new HttpClientResponseException(brokerStatus.getReason(), HttpResponse.status(brokerStatus))));
		String id = "urn:ngsi-ld:" + Organization.TYPE_ORGANIZATION + ":" + UUID.randomUUID();

		HttpClientResponseException exception = assertThrows(HttpClientResponseException.class,
				() -> httpClient.toBlocking().exchange(HttpRequest.DELETE(ORGANIZATION_PATH + id),
						Argument.of(String.class), Argument.of(ErrorDetails.class)));

		ErrorDetails details = exception.getResponse().getBody(ErrorDetails.class).orElseThrow();
		assertEquals(expectedStatus, exception.getStatus(), "Unexpected status.");
		assertEquals(expectedStatus.toString(), details.code(), "Unexpected error code.");
		assertEquals(expectedMessage, details.message(), "Unexpected error message.");
	}
}
