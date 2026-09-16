package org.fiware.tmforum.productcatalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.productcatalog.api.ProductOfferingApiTestClient;
import org.fiware.productcatalog.model.ProductOfferingCreateVO;
import org.fiware.productcatalog.model.ProductOfferingCreateVOTestExample;
import org.fiware.productcatalog.model.ProductOfferingVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for filtering productOfferings through the TMForum query parameters, focused on values that
 * contain characters with a special meaning in the NGSI-LD query language or in URLs.
 * <p>
 * Reproduces the reported defect that an entity whose property value contains parentheses -
 * e.g. {@code "name": "Test ProductOffering (ED)"} - cannot be selected via
 * {@code /productOffering?name=Test%20ProductOffering%20(ED)}. Blanks are covered as well, to show
 * that the parentheses, not the blanks, are the trigger.
 */
@MicronautTest(packages = { "org.fiware.tmforum.productcatalog" })
public class ProductOfferingFilterIT extends AbstractApiIT {

	/** NGSI-LD entity type the productOfferings are stored as - used to clean up between the tests. */
	private static final String PRODUCT_OFFERING_ENTITY_TYPE = "product-offering";

	/** Path of the productOffering collection, relative to the api's base path. */
	private static final String PRODUCT_OFFERING_PATH = "/productOffering";

	/** TMForum filter on the productOffering's name, e.g. {@code /productOffering?name=My%20Offering}. */
	private static final String NAME_FILTER_TEMPLATE = PRODUCT_OFFERING_PATH + "?name=%s";

	/**
	 * {@link URLEncoder} encodes blanks as "+", which is only valid for form encoded bodies. Query
	 * parameters of a GET request need them percent-encoded, exactly as a browser or curl would send them.
	 */
	private static final String FORM_ENCODED_BLANK = "+";
	private static final String PERCENT_ENCODED_BLANK = "%20";

	private final ProductOfferingApiTestClient productOfferingApiTestClient;
	private final HttpClient httpClient;

	public ProductOfferingFilterIT(ProductOfferingApiTestClient productOfferingApiTestClient,
			@Client("/") HttpClient httpClient, EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper,
			GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.productOfferingApiTestClient = productOfferingApiTestClient;
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
	 * A productOffering has to be selectable by its name, independent of the characters the name consists of.
	 *
	 * @param message      description of the concrete case
	 * @param offeringName name to create the productOffering with and to filter for afterwards
	 */
	@ParameterizedTest
	@MethodSource("provideNamesWithSpecialCharacters")
	public void listProductOfferingFilteredByName(String message, String offeringName) throws Exception {
		String createdId = createProductOffering(offeringName);

		HttpResponse<List<ProductOfferingVO>> filteredResponse = callAndCatch(() -> httpClient.toBlocking()
				.exchange(HttpRequest.GET(nameFilter(offeringName)), Argument.listOf(ProductOfferingVO.class)));

		assertEquals(HttpStatus.OK, filteredResponse.getStatus(), message);
		assertEquals(List.of(Map.entry(createdId, offeringName)),
				filteredResponse.getBody()
						.orElse(List.of())
						.stream()
						.map(offering -> Map.entry(offering.getId(), offering.getName()))
						.toList(),
				message);
	}

	private static Stream<Arguments> provideNamesWithSpecialCharacters() {
		return Stream.of(
				Arguments.of("A productOffering with a plain name should be filterable by that name.",
						"TestProductOfferingED"),
				Arguments.of("A productOffering with blanks in its name should be filterable by that name.",
						"Test ProductOffering ED"),
				Arguments.of("A productOffering with parentheses in its name should be filterable by that name.",
						"TestProductOffering(ED)"),
				Arguments.of(
						"A productOffering with parentheses and blanks in its name should be filterable by that name.",
						"Test ProductOffering (ED)"));
	}

	private String createProductOffering(String offeringName) {
		ProductOfferingCreateVO productOfferingCreateVO = ProductOfferingCreateVOTestExample.build()
				.atSchemaLocation(null)
				.name(offeringName)
				.productSpecification(null)
				.resourceCandidate(null)
				.serviceCandidate(null)
				.serviceLevelAgreement(null);
		return productOfferingApiTestClient.createProductOffering(null, productOfferingCreateVO).body().getId();
	}

	private String nameFilter(String offeringName) {
		return String.format(NAME_FILTER_TEMPLATE,
				URLEncoder.encode(offeringName, StandardCharsets.UTF_8)
						.replace(FORM_ENCODED_BLANK, PERCENT_ENCODED_BLANK));
	}
}
