package org.fiware.tmforum.resourcecatalog.rest;

import io.github.wistefan.mapping.JavaObjectMapper;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.resource.LogicalResourceSpecification;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the error response of the sub-type spec patch, which updates through the repository without the call-site
 * mapping, so every failed write answers 502.
 */
@MicronautTest(packages = {"org.fiware.tmforum.resourcecatalog"}, environments = "in-memory",
		contextBuilder = FirstMatchConfigContextBuilder.class)
class ResourceSpecificationApiControllerTest {

	private static final String SPECIFICATION_PATH = "/resourceSpecification/";
	private static final String NO_ANSWER_MESSAGE = "Connect Error: Connection refused";

	@Inject
	@Client("/")
	HttpClient httpClient;
	@Inject
	EntitiesApiClient entitiesApiClient;
	@Inject
	JavaObjectMapper javaObjectMapper;

	@MockBean(EntitiesApiClient.class)
	EntitiesApiClient entitiesApiClient() {
		return mock(EntitiesApiClient.class);
	}

	/** The broker's answer to the update, a status or (null) no answer at all. */
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

		Throwable exception() {
			return status == null ? new HttpClientException(NO_ANSWER_MESSAGE)
					: new HttpClientResponseException(status.getReason(), HttpResponse.status(status));
		}
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Failure.class)
	void subTypePatchFailsUncaught(Failure failure) {
		String id = "urn:ngsi-ld:" + LogicalResourceSpecification.TYPE_LOGICAL_RESOURCE_SPECIFICATION + ":"
				+ UUID.randomUUID();
		EntityVO stored = javaObjectMapper.toEntityVO(new LogicalResourceSpecification(id));
		when(entitiesApiClient.retrieveEntityById(any(), any(), any(), any(), any(), any()))
				.thenReturn(Mono.just(HttpResponse.ok(stored)));
		when(entitiesApiClient.updateEntity(any(), any(), any(), any())).thenReturn(Mono.error(failure.exception()));

		HttpClientResponseException exception = assertThrows(HttpClientResponseException.class,
				() -> httpClient.toBlocking().exchange(HttpRequest.PATCH(SPECIFICATION_PATH + id, "{}")
								.contentType(MediaType.APPLICATION_JSON_TYPE),
						Argument.of(String.class), Argument.of(ErrorDetails.class)));

		ErrorDetails details = exception.getResponse().getBody(ErrorDetails.class).orElseThrow();
		assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus(), "Unexpected status.");
		assertEquals(HttpStatus.BAD_GATEWAY.toString(), details.code(), "Unexpected error code.");
		assertEquals("Context broker is not reachable.", details.message(), "Unexpected error message.");
		verify(entitiesApiClient, atLeastOnce()).updateEntity(any(), any(), any(), any());
	}
}
