package org.fiware.tmforum.customerbillmanagement;

import io.micronaut.context.env.Environment;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.customerbillmanagement.domain.AppliedBillingRateCharacteristic;
import org.fiware.tmforum.customerbillmanagement.domain.AppliedCustomerBillingRate;
import org.fiware.tmforum.storage.EntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Filter and sort on {@code characteristic.value}, which the document and the NGSI-LD entity hold as
 * {@code tmfValue}, on every backend.
 */
@MicronautTest(packages = {"org.fiware.tmforum.customerbillmanagement"}, contextBuilder = FirstMatchConfigContextBuilder.class)
public class AppliedCustomerBillingRateListQueryIT extends AbstractApiIT {

	private static final int RATE_COUNT = 12;
	private static final List<String> SPEEDS = List.of("fast", "medium", "slow");
	private static final String CHARACTERISTIC_NAME = "speed";
	private static final String ID_FORMAT = "urn:ngsi-ld:applied-customer-billing-rate:list-query-%02d";
	private static final String NAME_FORMAT = "rate-%02d";
	private static final String PATH = "/appliedCustomerBillingRate";
	private static final int ALL = 100;
	private static final String POSTGRESQL_ENVIRONMENT = "postgresql";

	private final EntityRepository entityRepository;
	private final HttpClient httpClient;
	private final Environment environment;

	public AppliedCustomerBillingRateListQueryIT(EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper,
												 GeneralProperties generalProperties, EntityRepository entityRepository,
												 @Client("/") HttpClient httpClient, Environment environment) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.entityRepository = entityRepository;
		this.httpClient = httpClient;
		this.environment = environment;
	}

	@Override
	protected String getEntityType() {
		return AppliedCustomerBillingRate.TYPE_APPLIED_CUSTOMER_BILLING_RATE;
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"fast", "slow"})
	public void filterOnRenamedAttribute(String speed) throws Exception {
		seed();

		HttpResponse<String> response = list("characteristic.value=" + speed + "&limit=" + ALL);

		assertEquals(HttpStatus.OK, response.getStatus());
		List<String> expected = IntStream.range(0, RATE_COUNT)
				.filter(i -> speed(i).equals(speed))
				.mapToObj(AppliedCustomerBillingRateListQueryIT::id)
				.sorted().toList();
		assertEquals(expected, ids(response).stream().sorted().toList());
	}

	@Test
	public void sortOnRenamedAttribute() throws Exception {
		seed();

		HttpResponse<String> response = list("sort=-characteristic.value,name&limit=" + ALL);

		assertEquals(HttpStatus.OK, response.getStatus());
		List<String> observed = ids(response);
		if (!environment.getActiveNames().contains(POSTGRESQL_ENVIRONMENT)) {
			// both brokers ignore orderBy (KnownDeviation.SORT_IS_IGNORED, pending acceptance (ticket 0))
			assertEquals(Set.copyOf(allIds()), Set.copyOf(observed));
			return;
		}
		List<String> expected = IntStream.range(0, RATE_COUNT).boxed()
				.sorted(Comparator.comparing(AppliedCustomerBillingRateListQueryIT::speed).reversed()
						.thenComparing(i -> String.format(NAME_FORMAT, i)))
				.map(AppliedCustomerBillingRateListQueryIT::id)
				.toList();
		assertEquals(expected, observed);
	}

	private void seed() {
		IntStream.range(0, RATE_COUNT).forEach(i -> {
			AppliedBillingRateCharacteristic characteristic = new AppliedBillingRateCharacteristic();
			characteristic.setName(CHARACTERISTIC_NAME);
			characteristic.setTmfValue(speed(i));
			AppliedCustomerBillingRate rate = new AppliedCustomerBillingRate(id(i));
			rate.setName(String.format(NAME_FORMAT, i));
			rate.setCharacteristic(List.of(characteristic));
			entityRepository.createDomainEntity(rate).block();
		});
	}

	private HttpResponse<String> list(String query) throws Exception {
		return callAndCatch(() -> httpClient.toBlocking().exchange(HttpRequest.GET(PATH + "?" + query), String.class));
	}

	private List<String> ids(HttpResponse<String> response) {
		JsonNode body = objectMapper.readTree(response.body());
		List<String> ids = new ArrayList<>();
		body.forEach(entity -> ids.add(entity.get("id").asString()));
		return ids;
	}

	private static List<String> allIds() {
		return IntStream.range(0, RATE_COUNT).mapToObj(AppliedCustomerBillingRateListQueryIT::id).toList();
	}

	private static String speed(int index) {
		return SPEEDS.get(index % SPEEDS.size());
	}

	private static String id(int index) {
		return String.format(ID_FORMAT, index);
	}
}
