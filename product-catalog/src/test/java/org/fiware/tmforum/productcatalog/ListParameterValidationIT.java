package org.fiware.tmforum.productcatalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the validation of the typed query parameters on the list endpoints, covering both kinds
 * such a parameter can be of:
 * <ul>
 *     <li>the numeric pagination parameters {@code offset} and {@code limit}, declared on the api</li>
 *     <li>filters on attributes that are booleans or numbers on the queried entity</li>
 * </ul>
 * <p>
 * Both used to accept values that are not valid for their type: an invalid {@code offset}/{@code limit}
 * was silently ignored and answered with a 200 containing the default page, while an invalid filter
 * value produced a query the broker rejected, surfacing as a 500. Both have to be answered with a 400.
 */
@MicronautTest(packages = { "org.fiware.tmforum.productcatalog" })
public class ListParameterValidationIT extends AbstractApiIT {

	/** NGSI-LD entity type the productOfferings are stored as - used to clean up between the tests. */
	private static final String PRODUCT_OFFERING_ENTITY_TYPE = "product-offering";

	/** Path of the productOffering collection, relative to the api's base path. */
	private static final String PRODUCT_OFFERING_PATH = "/productOffering";

	/** Path of the productOfferingPrice collection, which carries the numeric attributes. */
	private static final String PRODUCT_OFFERING_PRICE_PATH = "/productOfferingPrice";

	private final HttpClient httpClient;

	public ListParameterValidationIT(@Client("/") HttpClient httpClient, EntitiesApiClient entitiesApiClient,
			ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.httpClient = httpClient;
	}

	@Override
	protected String getEntityType() {
		return PRODUCT_OFFERING_ENTITY_TYPE;
	}

	@MockBean(TMForumEventHandler.class)
	public TMForumEventHandler eventHandler() {
		TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);

		when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
		when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());

		return eventHandler;
	}

	/**
	 * A value that cannot be interpreted as the type of the parameter it is provided for is invalid
	 * input and has to be answered with a 400 - neither dropped and answered as if it had never been
	 * sent, nor forwarded to the broker as part of a query it cannot parse.
	 *
	 * @param message description of the concrete case
	 * @param path    path of the queried collection
	 * @param query   query string to append to the listing
	 */
	@ParameterizedTest
	@MethodSource("provideInvalidlyTypedParameters")
	public void listRejectsInvalidlyTypedParameters(String message, String path, String query) throws Exception {
		HttpResponse<String> response = callAndCatch(
				() -> httpClient.toBlocking().exchange(HttpRequest.GET(path + query), String.class));

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatus(), message);
		assertTrue(response.getBody(ErrorDetails.class).isPresent(), "Error details should be provided for " + query);
	}

	private static Stream<Arguments> provideInvalidlyTypedParameters() {
		return Stream.of(
				// pagination parameters, declared as integers on the api
				Arguments.of("A non-numeric limit should be rejected.", PRODUCT_OFFERING_PATH, "?limit=my-non-int"),
				Arguments.of("A non-numeric offset should be rejected.", PRODUCT_OFFERING_PATH, "?offset=my-non-int"),
				Arguments.of("A decimal limit should be rejected.", PRODUCT_OFFERING_PATH, "?limit=1.5"),
				Arguments.of("A decimal offset should be rejected.", PRODUCT_OFFERING_PATH, "?offset=1.5"),
				Arguments.of("Non-numeric offset and limit should be rejected.", PRODUCT_OFFERING_PATH,
						"?offset=my-non-int&limit=my-non-int"),
				// filters on attributes that are typed on the queried entity
				Arguments.of("A non-boolean value for a boolean attribute should be rejected.", PRODUCT_OFFERING_PATH,
						"?isBundle=my-non-bool"),
				Arguments.of("An upper-case boolean should be rejected.", PRODUCT_OFFERING_PATH, "?isBundle=True"),
				Arguments.of("A non-numeric value for a number attribute should be rejected.",
						PRODUCT_OFFERING_PRICE_PATH, "?percentage=my-non-number"),
				Arguments.of("NaN should be rejected for a number attribute.", PRODUCT_OFFERING_PRICE_PATH,
						"?percentage=NaN"),
				Arguments.of("A non-numeric value for an integer attribute should be rejected.",
						PRODUCT_OFFERING_PRICE_PATH, "?recurringChargePeriodLength=my-non-int"),
				Arguments.of("An invalid value inside a value list should be rejected.", PRODUCT_OFFERING_PATH,
						"?isBundle=true,my-non-bool"),
				// booleans have no order - forwarding such a query terminates Orion-LD 1.9.0
				Arguments.of("An ordering operator on a boolean attribute should be rejected.",
						PRODUCT_OFFERING_PATH, "?isBundle.gt=true"),
				Arguments.of("An ordering operator on a boolean attribute should be rejected.",
						PRODUCT_OFFERING_PATH, "?isBundle.lte=false"));
	}

	/**
	 * The rejection has to stay limited to the invalid values - everything that is valid for the
	 * parameter's type still has to be answered normally.
	 *
	 * @param message description of the concrete case
	 * @param path    path of the queried collection
	 * @param query   query string to append to the listing
	 */
	@ParameterizedTest
	@MethodSource("provideValidlyTypedParameters")
	public void listAcceptsValidlyTypedParameters(String message, String path, String query) throws Exception {
		HttpResponse<String> response = callAndCatch(
				() -> httpClient.toBlocking().exchange(HttpRequest.GET(path + query), String.class));

		assertEquals(HttpStatus.OK, response.getStatus(), message);
	}

	/**
	 * Value lists (e.g. {@code ?isBundle=true,false}) are deliberately not covered here: the broker
	 * rejects the list syntax this translates into for every attribute type, strings included, so
	 * they are broken independent of the type checks.
	 */
	private static Stream<Arguments> provideValidlyTypedParameters() {
		return Stream.of(
				Arguments.of("Valid pagination parameters should be accepted.", PRODUCT_OFFERING_PATH,
						"?offset=0&limit=10"),
				Arguments.of("Both boolean literals should be accepted.", PRODUCT_OFFERING_PATH, "?isBundle=true"),
				Arguments.of("Both boolean literals should be accepted.", PRODUCT_OFFERING_PATH, "?isBundle=false"),
				Arguments.of("A decimal number should be accepted.", PRODUCT_OFFERING_PRICE_PATH, "?percentage=1.5"),
				Arguments.of("A negative number should be accepted.", PRODUCT_OFFERING_PRICE_PATH,
						"?percentage=-1.5"),
				Arguments.of("An integer should be accepted.", PRODUCT_OFFERING_PRICE_PATH,
						"?recurringChargePeriodLength=12"),
				Arguments.of("A string filter should be unaffected by the type checks.", PRODUCT_OFFERING_PATH,
						"?name=my-offering"),
				Arguments.of("Ordering a number should still be accepted.", PRODUCT_OFFERING_PRICE_PATH,
						"?percentage.gt=1.5"),
				Arguments.of("Ordering a string should still be accepted.", PRODUCT_OFFERING_PATH,
						"?name.gt=my-offering"));
	}
}
