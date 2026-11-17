package org.fiware.tmforum.productcatalog;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.env.Environment;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.productcatalog.api.ProductOfferingApiTestClient;
import org.fiware.productcatalog.api.ProductSpecificationApiTestClient;
import org.fiware.productcatalog.model.ProductOfferingCreateVO;
import org.fiware.productcatalog.model.ProductOfferingCreateVOTestExample;
import org.fiware.productcatalog.model.ProductOfferingVO;
import org.fiware.productcatalog.model.ProductSpecificationCreateVO;
import org.fiware.productcatalog.model.ProductSpecificationCreateVOTestExample;
import org.fiware.productcatalog.model.ProductSpecificationRefVO;
import org.fiware.productcatalog.model.ProductSpecificationVO;
import org.fiware.productcatalog.model.RelatedPartyVO;
import org.fiware.productcatalog.model.TimePeriodVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.common.test.TestEntityCleaner;
import org.fiware.tmforum.product.ProductOffering;
import org.fiware.tmforum.product.ProductSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Filter, sort and paging through the list endpoints over a seeded catalog, on every backend. Expectations are
 * computed from the seeds; date-times compare as the stored strings (the date-time gate of ticket 90).
 */
@MicronautTest(packages = {"org.fiware.tmforum.productcatalog"}, contextBuilder = FirstMatchConfigContextBuilder.class)
@Property(name = "references.external-hosts", value = ListQueryIT.EXTERNAL_HOST)
public class ListQueryIT extends AbstractApiIT {

	static final String EXTERNAL_HOST = "parties.example.org";

	private static final int OFFERING_COUNT = 240;
	private static final int SPECIFICATION_COUNT = 3;
	// above the seed count, so one request returns every match
	private static final int ALL = 1000;
	private static final int PAGE_SIZE = 7;
	// extension values with one and two digits, so numeric and string order differ
	private static final int EXTENSION_NUMBER_MODULUS = 12;
	// every third lastUpdate carries a fraction, so string and instant order differ
	private static final int FRACTION_EVERY = 3;
	private static final Duration FRACTION = Duration.ofMillis(500);
	private static final Instant LAST_UPDATE_BASE = Instant.parse("2024-01-01T08:00:00Z");
	private static final Instant VALID_FOR_BASE = Instant.parse("2025-03-01T00:00:00Z");
	private static final List<String> LIFECYCLE_STATUS = List.of("Active", "Retired", "InDesign", "Obsolete");
	private static final List<String> VERSIONS = List.of("1.0", "1.1", "2.0");
	private static final List<String> AT_TYPES = List.of("ProductOffering", "BundledProductOffering");
	private static final List<String> PARTY_ROLES = List.of("Owner", "Supplier");

	private static final String PRODUCT_OFFERING_PATH = "/productOffering";
	private static final String PRODUCT_SPECIFICATION_PATH = "/productSpecification";
	private static final String SCHEMA_LOCATION = "http://localhost:3000/schemas/properties-extension-schema.json";
	private static final String EXTENSION_NUMBER = "extensionNumber";
	private static final String PARTY_HREF_FORMAT = "https://" + EXTERNAL_HOST + "/tmf-api/party/v4/individual/%s";
	private static final String PARTY_ID_FORMAT = "urn:ngsi-ld:individual:list-query-%d";
	private static final String SPECIFICATION_PLACEHOLDER_FORMAT = "{specification-%d}";
	private static final String NAME_FORMAT = "offering-%03d";
	private static final String TOTAL_COUNT_HEADER = "X-Total-Count";
	private static final String RESULT_COUNT_HEADER = "X-Result-Count";
	private static final String POSTGRESQL_ENVIRONMENT = "postgresql";
	private static final String SCORPIO_ENVIRONMENT = "scorpio";

	// seeded once for the class; the cleanup of the next IT class removes them
	private static List<Offering> offerings;
	private static List<String> specificationIds;

	private final ProductOfferingApiTestClient productOfferingApiTestClient;
	private final ProductSpecificationApiTestClient productSpecificationApiTestClient;
	private final HttpClient httpClient;
	private final TestEntityCleaner testEntityCleaner;
	private final Environment environment;
	private final Clock clock = mock(Clock.class);

	public ListQueryIT(ProductOfferingApiTestClient productOfferingApiTestClient,
					   ProductSpecificationApiTestClient productSpecificationApiTestClient,
					   @Client("/") HttpClient httpClient, EntitiesApiClient entitiesApiClient,
					   ObjectMapper objectMapper, GeneralProperties generalProperties,
					   TestEntityCleaner testEntityCleaner, Environment environment) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.productOfferingApiTestClient = productOfferingApiTestClient;
		this.productSpecificationApiTestClient = productSpecificationApiTestClient;
		this.httpClient = httpClient;
		this.testEntityCleaner = testEntityCleaner;
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

	// replaces the per-test cleanup: the seeds are shared by every case of the class
	@BeforeEach
	@Override
	public void cleanUp() {
		if (offerings != null) {
			return;
		}
		testEntityCleaner.deleteAll(ProductOffering.TYPE_PRODUCT_OFFERING);
		testEntityCleaner.deleteAll(ProductSpecification.TYPE_PRODUCT_SPECIFICATION);
		specificationIds = IntStream.range(0, SPECIFICATION_COUNT).mapToObj(this::createSpecification).toList();
		offerings = IntStream.range(0, OFFERING_COUNT).mapToObj(this::createOffering).toList();
	}

	/**
	 * One seeded offering, holding what the filters and sorts read.
	 */
	private record Offering(String id, int index, String name, String lifecycleStatus, String version,
							boolean isBundle, String atType, int extensionNumber, Instant lastUpdate,
							Instant validForStart, int specificationIndex) {

		// how every backend stores an instant
		String storedLastUpdate() {
			return lastUpdate.toString();
		}

		String storedValidForStart() {
			return validForStart.toString();
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("filterCases")
	public void filter(String filter, Predicate<Offering> matches) throws Exception {
		String query = withSpecificationIds(filter);
		HttpResponse<String> response = list(PRODUCT_OFFERING_PATH, query + "&limit=" + ALL);

		assertEquals(HttpStatus.OK, response.getStatus(), query);
		List<String> expected = offerings.stream().filter(matches).map(Offering::id).sorted().toList();
		assertEquals(expected, ids(response).stream().sorted().toList(), query);
		assertEquals(String.valueOf(expected.size()), response.header(RESULT_COUNT_HEADER), query);
	}

	private static Stream<Arguments> filterCases() {
		return Stream.of(
				filterCase(eq("lifecycleStatus", "Retired"), o -> o.lifecycleStatus().equals("Retired")),
				filterCase(eq("isBundle", "true"), Offering::isBundle),
				filterCase(eq("lifecycleStatus", "Active,Obsolete"),
						o -> Set.of("Active", "Obsolete").contains(o.lifecycleStatus())),
				filterCase(eq("lifecycleStatus", "Active") + ";" + eq("version", "2.0"),
						o -> o.lifecycleStatus().equals("Active") || o.version().equals("2.0")),
				filterCase(eq("lifecycleStatus", "Active") + "&" + eq("version", "2.0"),
						o -> o.lifecycleStatus().equals("Active") && o.version().equals("2.0")),
				filterCase(eq("lifecycleStatus", "Active,Retired") + "&" + eq("isBundle", "false"),
						o -> Set.of("Active", "Retired").contains(o.lifecycleStatus()) && !o.isBundle()),
				filterCase(eq(EXTENSION_NUMBER, "7"), o -> o.extensionNumber() == 7),
				filterCase(eq(EXTENSION_NUMBER + ".gt", "9"), o -> o.extensionNumber() > 9),
				filterCase(eq(EXTENSION_NUMBER + ".gte", "9"), o -> o.extensionNumber() >= 9),
				filterCase(eq(EXTENSION_NUMBER + ".lt", "2"), o -> o.extensionNumber() < 2),
				filterCase(eq(EXTENSION_NUMBER + ".lte", "2"), o -> o.extensionNumber() <= 2),
				filterCase(eq("@type", "BundledProductOffering"), o -> o.atType().equals("BundledProductOffering")),
				filterCase(eq("name.regex", "-0[0-4]"), o -> Pattern.compile("-0[0-4]").matcher(o.name()).find()),
				filterCase(eq("lifecycleStatus.regex", "^(?!Active$).*$"), o -> !o.lifecycleStatus().equals("Active")),
				// patterns the broker's q could lex as a number or a date-time
				filterCase(eq("version.regex", "2.0"), o -> Pattern.compile("2.0").matcher(o.version()).find()),
				filterCase(eq("validFor.startDateTime.regex", "2025-03-05"),
						o -> o.storedValidForStart().contains("2025-03-05")),
				filterCase(eq("validFor.startDateTime.gte", "2025-03-05T00:00:00Z"),
						o -> o.storedValidForStart().compareTo("2025-03-05T00:00:00Z") >= 0),
				filterCase(eq("validFor.startDateTime.lt", "2025-03-03T12:00:00Z"),
						o -> o.storedValidForStart().compareTo("2025-03-03T12:00:00Z") < 0),
				filterCase("productSpecification=" + specificationPlaceholder(1), o -> o.specificationIndex() == 1),
				filterCase("productSpecification.id=" + specificationPlaceholder(2), o -> o.specificationIndex() == 2),
				filterCase(eq(EXTENSION_NUMBER + ".gte", "3") + "&" + eq(EXTENSION_NUMBER + ".lt", "5"),
						o -> o.extensionNumber() >= 3 && o.extensionNumber() < 5));
	}

	/**
	 * A regex holding |,;() is rejected with a 400 on the brokers, whose q reads those characters as syntax
	 * (KnownDeviation.REGEX_HOLDING_QUERY_SYNTAX_IS_REJECTED, pending acceptance (ticket 0)); PostgreSQL matches it.
	 */
	@ParameterizedTest(name = "name.regex={0}")
	@ValueSource(strings = {"-00[1-3]|-01[0-2]", "-0(0|1)7"})
	public void filterOnRegexHoldingQuerySyntax(String pattern) throws Exception {
		String query = eq("name.regex", pattern);
		HttpResponse<String> response = list(PRODUCT_OFFERING_PATH, query + "&limit=" + ALL);

		if (!environment.getActiveNames().contains(POSTGRESQL_ENVIRONMENT)) {
			assertEquals(HttpStatus.BAD_REQUEST, response.getStatus(),
					"The broker takes the pattern now, drop REGEX_HOLDING_QUERY_SYNTAX_IS_REJECTED.");
			return;
		}
		assertEquals(HttpStatus.OK, response.getStatus(), query);
		Pattern compiled = Pattern.compile(pattern);
		List<String> expected = offerings.stream().filter(o -> compiled.matcher(o.name()).find())
				.map(Offering::id).sorted().toList();
		assertEquals(expected, ids(response).stream().sorted().toList(), query);
	}

	/**
	 * A number and a boolean comparison in one filter. Scorpio fails it (KnownDeviation.NUMBER_AND_BOOLEAN_FILTER_FAILS,
	 * pending acceptance (ticket 0)), so there the case pins the 500.
	 */
	@Test
	public void filterOnNumberAndBoolean() throws Exception {
		String query = eq(EXTENSION_NUMBER + ".gte", "3") + "&" + eq(EXTENSION_NUMBER + ".lt", "5")
				+ "&" + eq("isBundle", "true");
		HttpResponse<String> response = list(PRODUCT_OFFERING_PATH, query + "&limit=" + ALL);

		if (environment.getActiveNames().contains(SCORPIO_ENVIRONMENT)) {
			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus(),
					"Scorpio answers the filter now, drop NUMBER_AND_BOOLEAN_FILTER_FAILS.");
			return;
		}
		assertEquals(HttpStatus.OK, response.getStatus(), query);
		List<String> expected = offerings.stream()
				.filter(o -> o.extensionNumber() >= 3 && o.extensionNumber() < 5 && o.isBundle())
				.map(Offering::id).sorted().toList();
		assertEquals(expected, ids(response).stream().sorted().toList(), query);
	}

	/**
	 * Date-time comparisons on lastUpdate, seeded with and without a fraction, against literals in Z, offset and
	 * date-only form: every backend compares the stored strings, so the expectation is a string comparison.
	 */
	@ParameterizedTest(name = "lastUpdate{0}={1}")
	@MethodSource("dateTimeCases")
	public void filterOnLastUpdate(String operator, String literal) throws Exception {
		String query = eq("lastUpdate" + operator, literal);
		HttpResponse<String> response = list(PRODUCT_OFFERING_PATH, query + "&limit=" + ALL);

		assertEquals(HttpStatus.OK, response.getStatus(), query);
		List<String> expected = offerings.stream()
				.filter(o -> compares(operator, o.storedLastUpdate().compareTo(literal)))
				.map(Offering::id).sorted().toList();
		assertEquals(expected, ids(response).stream().sorted().toList(), query);
	}

	private static Stream<Arguments> dateTimeCases() {
		// the seeds run from 08:00 to 11:59 on 2024-01-01; 10:00:00 is stored whole, 10:01:00 with a fraction
		List<String> literals = List.of("2024-01-01T10:00:00Z", "2024-01-01T10:01:00Z",
				"2024-01-01T11:00:00+01:00", "2024-01-01");
		return Stream.of(".gt", ".gte", ".lt", ".lte")
				.flatMap(operator -> literals.stream().map(literal -> Arguments.of(operator, literal)));
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"Owner", "Supplier"})
	public void filterOnNestedListPath(String role) throws Exception {
		String query = eq("relatedParty.role", role);
		HttpResponse<String> response = list(PRODUCT_SPECIFICATION_PATH, query + "&limit=" + ALL);

		assertEquals(HttpStatus.OK, response.getStatus(), query);
		List<String> expected = IntStream.range(0, SPECIFICATION_COUNT)
				.filter(i -> partyRole(i).equals(role))
				.mapToObj(specificationIds::get).sorted().toList();
		assertEquals(expected, ids(response).stream().sorted().toList(), query);
	}

	@ParameterizedTest(name = "sort={0}")
	@MethodSource("sortCases")
	public void sort(String sort, Comparator<Offering> order) throws Exception {
		HttpResponse<String> response = list(PRODUCT_OFFERING_PATH, "sort=" + sort + "&limit=" + ALL);

		assertEquals(HttpStatus.OK, response.getStatus(), sort);
		List<String> observed = ids(response);
		if (sortIgnored()) {
			assertEquals(Set.copyOf(allIds()), Set.copyOf(observed), sort);
			return;
		}
		assertEquals(offerings.stream().sorted(order).map(Offering::id).toList(), observed, sort);
	}

	private static Stream<Arguments> sortCases() {
		Comparator<Offering> byName = Comparator.comparing(Offering::name);
		Comparator<Offering> byLastUpdate = Comparator.comparing(Offering::storedLastUpdate);
		return Stream.of(
				Arguments.of("name", byName),
				Arguments.of("-name", byName.reversed()),
				Arguments.of("lastUpdate", byLastUpdate),
				Arguments.of("-lastUpdate", byLastUpdate.reversed()));
	}

	/**
	 * Pages through a filtered, sorted result: the pages join to the full result without duplicates or gaps, and
	 * each carries the total and its own count.
	 */
	@Test
	public void pageThroughFilteredSortedResult() throws Exception {
		String query = eq("lifecycleStatus", "Active,Retired") + "&sort=-name";
		List<Offering> matching = offerings.stream()
				.filter(o -> Set.of("Active", "Retired").contains(o.lifecycleStatus()))
				.sorted(Comparator.comparing(Offering::name).reversed())
				.toList();

		List<String> paged = new ArrayList<>();
		for (int offset = 0; offset < matching.size(); offset += PAGE_SIZE) {
			HttpResponse<String> page = list(PRODUCT_OFFERING_PATH,
					query + "&offset=" + offset + "&limit=" + PAGE_SIZE);
			int expectedCount = Math.min(PAGE_SIZE, matching.size() - offset);
			assertEquals(String.valueOf(matching.size()), page.header(TOTAL_COUNT_HEADER), "offset " + offset);
			assertEquals(String.valueOf(expectedCount), page.header(RESULT_COUNT_HEADER), "offset " + offset);
			paged.addAll(ids(page));
		}

		assertEquals(paged.size(), new HashSet<>(paged).size(), "No entity may show up on two pages.");
		List<String> expected = matching.stream().map(Offering::id).toList();
		if (sortIgnored()) {
			assertEquals(Set.copyOf(expected), Set.copyOf(paged), query);
			return;
		}
		assertEquals(expected, paged, query);
	}

	private String createSpecification(int index) {
		ProductSpecificationCreateVO createVO = ProductSpecificationCreateVOTestExample.build()
				.atSchemaLocation(null)
				.targetProductSchema(null)
				.relatedParty(List.of(new RelatedPartyVO()
						.id(String.format(PARTY_ID_FORMAT, index))
						.href(URI.create(String.format(PARTY_HREF_FORMAT, index)))
						.role(partyRole(index))));
		try {
			HttpResponse<ProductSpecificationVO> created = callAndCatch(
					() -> productSpecificationApiTestClient.createProductSpecification(null, createVO));
			assertEquals(HttpStatus.CREATED, created.getStatus(), "The specification should have been created.");
			return created.body().getId();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private Offering createOffering(int index) {
		Instant lastUpdate = LAST_UPDATE_BASE.plus(Duration.ofMinutes(index))
				.plus(index % FRACTION_EVERY == 1 ? FRACTION : Duration.ZERO);
		Offering seed = new Offering(null, index, String.format(NAME_FORMAT, index),
				LIFECYCLE_STATUS.get(index % LIFECYCLE_STATUS.size()), VERSIONS.get(index % VERSIONS.size()),
				index % 2 == 0, AT_TYPES.get(index % AT_TYPES.size()), index % EXTENSION_NUMBER_MODULUS,
				lastUpdate, VALID_FOR_BASE.plus(Duration.ofHours(index)),
				index % SPECIFICATION_COUNT);
		ProductOfferingCreateVO createVO = (ProductOfferingCreateVO) ProductOfferingCreateVOTestExample.build()
				.atSchemaLocation(URI.create(SCHEMA_LOCATION))
				.name(seed.name())
				.lifecycleStatus(seed.lifecycleStatus())
				.version(seed.version())
				.isBundle(seed.isBundle())
				.atType(seed.atType())
				.validFor(new TimePeriodVO().startDateTime(seed.validForStart()))
				.productSpecification(new ProductSpecificationRefVO().id(specificationIds.get(seed.specificationIndex())))
				.resourceCandidate(null)
				.serviceCandidate(null)
				.serviceLevelAgreement(null)
				.unknownProperties(Map.of(EXTENSION_NUMBER, seed.extensionNumber()));
		when(clock.instant()).thenReturn(lastUpdate);
		try {
			HttpResponse<ProductOfferingVO> created = callAndCatch(
					() -> productOfferingApiTestClient.createProductOffering(null, createVO));
			assertEquals(HttpStatus.CREATED, created.getStatus(), "The offering should have been created.");
			return new Offering(created.body().getId(), index, seed.name(), seed.lifecycleStatus(), seed.version(),
					seed.isBundle(), seed.atType(), seed.extensionNumber(), lastUpdate, seed.validForStart(),
					seed.specificationIndex());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private HttpResponse<String> list(String path, String query) throws Exception {
		return callAndCatch(() -> httpClient.toBlocking().exchange(HttpRequest.GET(path + "?" + query), String.class));
	}

	private List<String> ids(HttpResponse<String> response) {
		JsonNode body = objectMapper.readTree(response.body());
		assertTrue(body.isArray(), "The list should answer an array: " + response.body());
		List<String> ids = new ArrayList<>();
		body.forEach(entity -> ids.add(entity.get("id").asString()));
		return ids;
	}

	private List<String> allIds() {
		return offerings.stream().map(Offering::id).toList();
	}

	// both brokers ignore orderBy (KnownDeviation.SORT_IS_IGNORED, pending acceptance (ticket 0))
	private boolean sortIgnored() {
		return !environment.getActiveNames().contains(POSTGRESQL_ENVIRONMENT);
	}

	private static boolean compares(String operator, int comparison) {
		return switch (operator) {
			case ".gt" -> comparison > 0;
			case ".gte" -> comparison >= 0;
			case ".lt" -> comparison < 0;
			case ".lte" -> comparison <= 0;
			default -> throw new IllegalArgumentException(operator);
		};
	}

	private static String partyRole(int specificationIndex) {
		return PARTY_ROLES.get(specificationIndex % PARTY_ROLES.size());
	}

	// the cases are built before the seeds exist, so they name a specification by its index
	private static String specificationPlaceholder(int index) {
		return String.format(SPECIFICATION_PLACEHOLDER_FORMAT, index);
	}

	private static String withSpecificationIds(String filter) {
		String query = filter;
		for (int i = 0; i < SPECIFICATION_COUNT; i++) {
			query = query.replace(specificationPlaceholder(i), specificationIds.get(i));
		}
		return query;
	}

	private static Arguments filterCase(String query, Predicate<Offering> matches) {
		return Arguments.of(query, matches);
	}

	// encodes the value only: ',' stays the TMForum value separator
	private static String eq(String attribute, String value) {
		return URLEncoder.encode(attribute, StandardCharsets.UTF_8) + "="
				+ URLEncoder.encode(value, StandardCharsets.UTF_8).replace("%2C", ",");
	}
}
