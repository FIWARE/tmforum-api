package org.fiware.tmforum.productcatalog;

import io.micronaut.context.env.Environment;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.productcatalog.api.ProductOfferingApiTestClient;
import org.fiware.productcatalog.model.ProductOfferingCreateVO;
import org.fiware.productcatalog.model.ProductOfferingCreateVOTestExample;
import org.fiware.productcatalog.model.ProductOfferingVO;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.BrokerOnly;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.product.ProductOffering;
import org.fiware.tmforum.storage.query.DateTimeLiterals;
import org.junit.jupiter.api.Test;
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
 * Characterises how a date-time filter or sort on {@code lastUpdate} behaves on the brokers today: as instants or as
 * the stored strings. The API path measures today's renderer and broker together, the renderer quoting the literal
 * (a string in the NGSI-LD q grammar); the probe sends it unquoted (a date-time) straight to the broker.
 * Seeds values with and without a fraction, so string and instant semantics give different results.
 * Input for the date-time gate of ticket 90; it pins today's behaviour, not what it should be.
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
	// creation order differs from both instant and string order, so neither can pass by accident
	private static final List<Instant> SEEDS = List.of(EXACT, EARLY, FRACTION);

	private final ProductOfferingApiTestClient productOfferingApiTestClient;
	private final HttpClient httpClient;
	private final EntitiesApiClient entitiesApiClient;
	private final Environment environment;
	private final String tenant;
	private final String linkHeader;
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
		EQ(".eq", "==", c -> c == 0),
		GT(".gt", ">", c -> c > 0),
		GTE(".gte", ">=", c -> c >= 0),
		LT(".lt", "<", c -> c < 0),
		LTE(".lte", "<=", c -> c <= 0);

		private final String suffix;
		private final String ngsiLdOperator;
		private final IntPredicate matches;

		FilterOperator(String suffix, String ngsiLdOperator, IntPredicate matches) {
			this.suffix = suffix;
			this.ngsiLdOperator = ngsiLdOperator;
			this.matches = matches;
		}
	}

	/**
	 * How a broker compares a date-time: by instant, lexically by the stored string, never matching, not at all (the
	 * sort direction changes nothing) or by failing the request.
	 */
	enum Semantics {
		INSTANT,
		STRING,
		NONE,
		IGNORED,
		UNSUPPORTED
	}

	/**
	 * What each broker does today with the quoted literal the API sends, per literal form.
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
	/**
	 * What each broker does with an unquoted literal sent straight to it, per literal form. The mapping stores
	 * lastUpdate as a plain string, not a typed DateTime: Orion-LD 1.9.0 then matches nothing, Scorpio 5.0.3 still
	 * compares the strings. The date-only literal precedes every seed either way, so it cannot tell the two apart.
	 */
	private static final Map<String, Map<LiteralForm, Semantics>> OBSERVED_UNQUOTED_FILTER_SEMANTICS = Map.of(
			ORION_LD_ENVIRONMENT, Map.of(
					LiteralForm.ZULU, Semantics.NONE,
					LiteralForm.OFFSET, Semantics.NONE,
					LiteralForm.LOCAL, Semantics.NONE,
					LiteralForm.DATE, Semantics.NONE),
			SCORPIO_ENVIRONMENT, Map.of(
					LiteralForm.ZULU, Semantics.STRING,
					LiteralForm.OFFSET, Semantics.STRING,
					LiteralForm.LOCAL, Semantics.STRING,
					LiteralForm.DATE, Semantics.STRING));
	// Orion-LD 1.9.0 rejects orderBy without local=true (501), which the API passes on as a 500;
	// Scorpio 5.0.3 answers in creation order whatever the direction.
	// Both pin a known defect, not a contract: a human still has to decide on fixing broker sort (ticket 90),
	// and a fix such as sending local=true turns this red on purpose; update the entry then.
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
		this.tenant = generalProperties.getTenant();
		this.linkHeader = String.format(CommonConstants.JSON_LD_CONTEXT_LINK_TEMPLATE, generalProperties.getContextUrl());
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
		Map<Semantics, Set<String>> expected = expectedMatches(seeds, form, operator);
		log.info("Date-time filter on {}: {} {} -> {} (instant: {}, string: {})", broker(), form, operator,
				names(seeds, observed), names(seeds, expected.get(Semantics.INSTANT)),
				names(seeds, expected.get(Semantics.STRING)));

		assertEquals(expected.get(OBSERVED_FILTER_SEMANTICS.get(broker()).get(form)), observed, query);
	}

	private static Stream<Arguments> filterCases() {
		return Arrays.stream(LiteralForm.values())
				.flatMap(form -> Arrays.stream(FilterOperator.values()).map(operator -> Arguments.of(form, operator)));
	}

	/**
	 * Sends the literal unquoted straight to the broker, bypassing the renderer: tells whether the broker can
	 * compare instants at all.
	 */
	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("filterCases")
	public void probeUnquotedFilterOnLastUpdate(LiteralForm form, FilterOperator operator) throws Exception {
		Map<String, Seed> seeds = seed();
		String q = LAST_UPDATE + operator.ngsiLdOperator + form.literal;
		Semantics expected = OBSERVED_UNQUOTED_FILTER_SEMANTICS.get(broker()).get(form);

		Set<String> observed;
		try {
			observed = entitiesApiClient.queryEntities(tenant, null, null, getEntityType(), null, q, null, null, null,
							null, null, null, null, null, null, null, linkHeader)
					.block().body().stream()
					.map(entity -> entity.getId().toString())
					.collect(Collectors.toSet());
		} catch (HttpClientResponseException e) {
			assertEquals(Semantics.UNSUPPORTED, expected, q + " failed with " + e.getStatus());
			return;
		}
		Map<Semantics, Set<String>> candidates = expectedMatches(seeds, form, operator);
		assertEquals(candidates.get(expected), observed, q + " -> " + names(seeds, observed) + ", instant: "
				+ names(seeds, candidates.get(Semantics.INSTANT)) + ", string: "
				+ names(seeds, candidates.get(Semantics.STRING)));
	}

	@Test
	public void sortOnLastUpdate() throws Exception {
		Map<String, Seed> seeds = seed();
		String ascendingSort = LAST_UPDATE;
		String descendingSort = "-" + LAST_UPDATE;

		HttpResponse<String> ascending = sortBy(ascendingSort);
		HttpResponse<String> descending = sortBy(descendingSort);

		Semantics semantics = OBSERVED_SORT_SEMANTICS.get(broker());
		if (semantics == Semantics.UNSUPPORTED) {
			String knownDefect = " still fails with a 500; if broker sort was fixed, update OBSERVED_SORT_SEMANTICS.";
			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ascending.getStatus(), ascendingSort + knownDefect);
			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, descending.getStatus(), descendingSort + knownDefect);
			return;
		}
		assertEquals(HttpStatus.OK, ascending.getStatus(), ascendingSort);
		assertEquals(HttpStatus.OK, descending.getStatus(), descendingSort);
		List<String> ascendingIds = ids(ascending.body()).toList();
		List<String> descendingIds = ids(descending.body()).toList();
		if (semantics == Semantics.IGNORED) {
			// the unsorted order is unspecified, so only pin that the sort is not applied
			assertEquals(seeds.keySet(), Set.copyOf(ascendingIds), ascendingSort);
			assertEquals(ascendingIds, descendingIds, "Both directions should answer alike.");
			return;
		}
		Comparator<Seed> order = semantics == Semantics.INSTANT
				? Comparator.comparing(Seed::instant)
				: Comparator.comparing(Seed::stored);
		assertEquals(sorted(seeds, order, false), ascendingIds, ascendingSort);
		assertEquals(sorted(seeds, order, true), descendingIds, descendingSort);
	}

	private HttpResponse<String> sortBy(String sort) throws Exception {
		return callAndCatch(() -> httpClient.toBlocking()
				.exchange(HttpRequest.GET(PRODUCT_OFFERING_PATH + "?sort=" + sort), String.class));
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

	private static Map<Semantics, Set<String>> expectedMatches(Map<String, Seed> seeds, LiteralForm form,
																FilterOperator operator) {
		Instant literalInstant = DateTimeLiterals.parse(form.literal).orElseThrow().toInstant();
		return Map.of(
				Semantics.NONE, Set.of(),
				Semantics.INSTANT, matching(seeds, seed -> operator.matches.test(seed.instant().compareTo(literalInstant))),
				Semantics.STRING, matching(seeds, seed -> operator.matches.test(seed.stored().compareTo(form.literal))));
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
