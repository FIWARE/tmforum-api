package org.fiware.tmforum.productcatalog;

import io.micronaut.context.env.Environment;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.productcatalog.api.ProductOfferingApiTestClient;
import org.fiware.productcatalog.model.ProductOfferingCreateVO;
import org.fiware.productcatalog.model.ProductOfferingCreateVOTestExample;
import org.fiware.productcatalog.model.ProductOfferingVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.BrokerOnly;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.product.ProductOffering;
import org.fiware.tmforum.storage.query.DateTimeLiterals;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Characterises how the brokers compare a date-time filter or sort on {@code lastUpdate}: as instants or as the
 * stored strings. Seeds values with and without a fraction, so string and instant semantics give different results.
 * Input for the date-time gate of ticket 90; it pins what the brokers do today, not what they should do.
 */
@Slf4j
@BrokerOnly
@MicronautTest(packages = {"org.fiware.tmforum.productcatalog"}, contextBuilder = FirstMatchConfigContextBuilder.class)
public class DateTimeFilterBehaviourIT extends AbstractApiIT {

	private static final String PRODUCT_OFFERING_PATH = "/productOffering";
	private static final String LAST_UPDATE = "lastUpdate";
	private static final String NGSI_LD_VALUE = "value";
	private static final String JSON_LD_VALUE = "@value";
	private static final String ID = "id";
	private static final String FRACTION_SEPARATOR = ".";
	private static final String ORION_LD_ENVIRONMENT = "orion-ld";
	private static final String SCORPIO_ENVIRONMENT = "scorpio";

	// Instant.toString() writes a zero fraction as "...00Z", so EARLY and EXACT are stored without one
	private static final Instant EARLY = Instant.parse("2024-01-01T09:00:00Z");
	private static final Instant EXACT = Instant.parse("2024-01-01T10:00:00Z");
	private static final Instant FRACTION = Instant.parse("2024-01-01T10:00:00.500Z");
	// creation order differs from both instant and string order, so a broker ignoring the sort is told apart
	private static final List<Instant> SEEDS = List.of(EXACT, EARLY, FRACTION);

	private final ProductOfferingApiTestClient productOfferingApiTestClient;
	private final HttpClient httpClient;
	private final EntitiesApiClient entitiesApiClient;
	private final Environment environment;
	private final Clock clock = mock(Clock.class);

	/**
	 * The forms a client can write the reference instant {@link #EXACT} in. Local and date-only literals mean UTC.
	 */
	enum LiteralForm {
		ZULU("2024-01-01T10:00:00Z"),
		OFFSET("2024-01-01T11:00:00+01:00"),
		LOCAL("2024-01-01T10:00:00"),
		DATE("2024-01-01");

		private final String literal;

		LiteralForm(String literal) {
			this.literal = literal;
		}
	}

	/**
	 * TMForum filter operators, with the outcome of a {@code compareTo(literal)} that matches.
	 */
	enum FilterOperator {
		EQ(".eq", c -> c == 0),
		GT(".gt", c -> c > 0),
		GTE(".gte", c -> c >= 0),
		LT(".lt", c -> c < 0),
		LTE(".lte", c -> c <= 0);

		private final String suffix;
		private final IntPredicate matches;

		FilterOperator(String suffix, IntPredicate matches) {
			this.suffix = suffix;
			this.matches = matches;
		}
	}

	/**
	 * How a broker compares a date-time: by instant, lexically by the stored string, not at all (results in
	 * creation order) or by failing the request.
	 */
	enum Semantics {
		INSTANT,
		STRING,
		IGNORED,
		UNSUPPORTED
	}

	/**
	 * What each broker does today, per literal form.
	 */
	private static final Map<String, Map<LiteralForm, Semantics>> OBSERVED_FILTER_SEMANTICS = Map.of(
			ORION_LD_ENVIRONMENT, Map.of(
					LiteralForm.ZULU, Semantics.STRING,
					LiteralForm.OFFSET, Semantics.STRING,
					LiteralForm.LOCAL, Semantics.STRING,
					LiteralForm.DATE, Semantics.STRING),
			SCORPIO_ENVIRONMENT, Map.of(
					LiteralForm.ZULU, Semantics.STRING,
					LiteralForm.OFFSET, Semantics.STRING,
					LiteralForm.LOCAL, Semantics.STRING,
					LiteralForm.DATE, Semantics.STRING));
	// Orion-LD 1.9.0 rejects orderBy without local=true (501), which the API passes on as a 500;
	// Scorpio 5.0.3 answers in creation order whatever the direction
	private static final Map<String, Semantics> OBSERVED_SORT_SEMANTICS = Map.of(
			ORION_LD_ENVIRONMENT, Semantics.UNSUPPORTED,
			SCORPIO_ENVIRONMENT, Semantics.IGNORED);

	public DateTimeFilterBehaviourIT(ProductOfferingApiTestClient productOfferingApiTestClient,
									 @Client("/") HttpClient httpClient, EntitiesApiClient entitiesApiClient,
									 ObjectMapper objectMapper, GeneralProperties generalProperties,
									 Environment environment) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.productOfferingApiTestClient = productOfferingApiTestClient;
		this.httpClient = httpClient;
		this.entitiesApiClient = entitiesApiClient;
		this.environment = environment;
	}

	@MockBean(Clock.class)
	public Clock clock() {
		return clock;
	}

	@MockBean(TMForumEventHandler.class)
	public TMForumEventHandler eventHandler() {
		TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);
		when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
		when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());
		return eventHandler;
	}

	@Override
	protected String getEntityType() {
		return ProductOffering.TYPE_PRODUCT_OFFERING;
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("filterCases")
	public void filterOnLastUpdate(LiteralForm form, FilterOperator operator) throws Exception {
		Map<String, Seed> seeds = seed();
		String query = LAST_UPDATE + operator.suffix + "=" + URLEncoder.encode(form.literal, StandardCharsets.UTF_8);

		HttpResponse<String> response = callAndCatch(() -> httpClient.toBlocking()
				.exchange(HttpRequest.GET(PRODUCT_OFFERING_PATH + "?" + query), String.class));

		assertEquals(HttpStatus.OK, response.getStatus(), query);
		Set<String> observed = ids(response.body()).collect(Collectors.toSet());
		Instant literalInstant = DateTimeLiterals.parse(form.literal).orElseThrow().toInstant();
		Map<Semantics, Set<String>> expected = Map.of(
				Semantics.INSTANT, matching(seeds, seed -> operator.matches.test(seed.instant().compareTo(literalInstant))),
				Semantics.STRING, matching(seeds, seed -> operator.matches.test(seed.stored().compareTo(form.literal))));
		log.info("Date-time filter on {}: {} {} -> {} (instant: {}, string: {})", broker(), form, operator,
				names(seeds, observed), names(seeds, expected.get(Semantics.INSTANT)),
				names(seeds, expected.get(Semantics.STRING)));

		assertEquals(expected.get(OBSERVED_FILTER_SEMANTICS.get(broker()).get(form)), observed, query);
	}

	private static Stream<Arguments> filterCases() {
		return Arrays.stream(LiteralForm.values())
				.flatMap(form -> Arrays.stream(FilterOperator.values()).map(operator -> Arguments.of(form, operator)));
	}

	@ParameterizedTest(name = "descending: {0}")
	@MethodSource("sortCases")
	public void sortOnLastUpdate(boolean descending) throws Exception {
		Map<String, Seed> seeds = seed();
		String sort = descending ? "-" + LAST_UPDATE : LAST_UPDATE;

		HttpResponse<String> response = callAndCatch(() -> httpClient.toBlocking()
				.exchange(HttpRequest.GET(PRODUCT_OFFERING_PATH + "?sort=" + sort), String.class));

		if (OBSERVED_SORT_SEMANTICS.get(broker()) == Semantics.UNSUPPORTED) {
			log.info("Date-time sort on {}: {} -> {}", broker(), sort, response.getStatus());
			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus(), sort);
			return;
		}
		assertEquals(HttpStatus.OK, response.getStatus(), sort);
		List<String> observed = ids(response.body()).toList();
		Map<Semantics, List<String>> expected = Map.of(
				Semantics.INSTANT, sorted(seeds, Comparator.comparing(Seed::instant), descending),
				Semantics.STRING, sorted(seeds, Comparator.comparing(Seed::stored), descending),
				Semantics.IGNORED, List.copyOf(seeds.keySet()));
		log.info("Date-time sort on {}: {} -> {} (instant: {}, string: {})", broker(), sort,
				names(seeds, observed), names(seeds, expected.get(Semantics.INSTANT)),
				names(seeds, expected.get(Semantics.STRING)));

		assertEquals(expected.get(OBSERVED_SORT_SEMANTICS.get(broker())), observed, sort);
	}

	private static Stream<Arguments> sortCases() {
		return Stream.of(Arguments.of(false), Arguments.of(true));
	}

	/**
	 * Creates one offering per seed instant and reads back the string each broker stored, keyed by entity id.
	 */
	private Map<String, Seed> seed() throws Exception {
		Map<String, Seed> seeds = new LinkedHashMap<>();
		for (Instant instant : SEEDS) {
			when(clock.instant()).thenReturn(instant);
			ProductOfferingCreateVO createVO = ProductOfferingCreateVOTestExample.build().atSchemaLocation(null)
					.productSpecification(null)
					.resourceCandidate(null)
					.serviceCandidate(null)
					.serviceLevelAgreement(null)
					.name(instant.toString());
			HttpResponse<ProductOfferingVO> created = callAndCatch(
					() -> productOfferingApiTestClient.createProductOffering(null, createVO));
			assertEquals(HttpStatus.CREATED, created.getStatus(), "The seed should have been created.");
			String id = created.body().getId();
			seeds.put(id, new Seed(instant, storedLastUpdate(id)));
		}
		assertEquals(Set.of(true, false), seeds.values().stream()
						.map(seed -> seed.stored().contains(FRACTION_SEPARATOR))
						.collect(Collectors.toSet()),
				"The stored values have to differ in precision: " + seeds.values());
		return seeds;
	}

	private String storedLastUpdate(String id) {
		JsonNode value = objectMapper.valueToTree(entitiesApiClient
						.retrieveEntityById(URI.create(id), null, null, null, null, null).block().body()
						.getAdditionalProperties().get(LAST_UPDATE))
				.get(NGSI_LD_VALUE);
		return value.isObject() ? value.get(JSON_LD_VALUE).asString() : value.asString();
	}

	private Stream<String> ids(String body) {
		return StreamSupport.stream(objectMapper.readTree(body).spliterator(), false)
				.map(offering -> offering.get(ID).asString());
	}

	private String broker() {
		return Stream.of(ORION_LD_ENVIRONMENT, SCORPIO_ENVIRONMENT)
				.filter(environment.getActiveNames()::contains)
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Runs only on a broker: " + environment.getActiveNames()));
	}

	private static Set<String> matching(Map<String, Seed> seeds, java.util.function.Predicate<Seed> predicate) {
		return seeds.entrySet().stream()
				.filter(entry -> predicate.test(entry.getValue()))
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
	}

	private static List<String> sorted(Map<String, Seed> seeds, Comparator<Seed> order, boolean descending) {
		Comparator<Seed> directed = descending ? order.reversed() : order;
		return seeds.entrySet().stream()
				.sorted(Map.Entry.comparingByValue(directed))
				.map(Map.Entry::getKey)
				.toList();
	}

	private static List<String> names(Map<String, Seed> seeds, Iterable<String> ids) {
		return StreamSupport.stream(ids.spliterator(), false)
				.map(id -> seeds.get(id).stored())
				.toList();
	}

	private record Seed(Instant instant, String stored) {
	}
}
