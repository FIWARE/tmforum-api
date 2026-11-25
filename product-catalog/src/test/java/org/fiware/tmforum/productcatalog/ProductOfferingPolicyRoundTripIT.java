package org.fiware.tmforum.productcatalog;

import io.github.wistefan.mapping.UnmappedProperty;
import io.micronaut.cache.CacheManager;
import io.micronaut.context.env.Environment;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.common.test.TestEntityCleaner;
import org.fiware.tmforum.product.ProductOffering;
import org.fiware.tmforum.product.ProductOfferingTerm;
import org.fiware.tmforum.product.ProductSpecification;
import org.fiware.tmforum.product.ProductSpecificationCharacteristic;
import org.fiware.tmforum.service.CharacteristicValueSpecification;
import org.fiware.tmforum.storage.EntityReadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AP-01: pins whether an ODRL policy on a product offering term or a product specification characteristic value
 * survives POST -> storage -> GET, and whether it is reachable on the domain object the storage port returns.
 * Subtrees compare by {@link JsonNode#equals}: key order is ignored, array order, value types and keys are not.
 * UNVERIFIED: no run on any backend has happened yet, so the stored outcomes below are the plan's starting
 * hypothesis, not observations; pin what each of the five runs reports before the findings use them.
 */
@MicronautTest(packages = {"org.fiware.tmforum.productcatalog"}, contextBuilder = FirstMatchConfigContextBuilder.class)
public class ProductOfferingPolicyRoundTripIT extends AbstractApiIT {

	private static final String ORION_LD = "orion-ld";
	private static final String SCORPIO = "scorpio";
	private static final String POSTGRESQL = CommonConstants.POSTGRESQL_ENVIRONMENT;
	private static final String REDIS = "redis";
	private static final String REDIS_SUFFIX = "+" + REDIS;
	private static final List<String> STORAGES = List.of(ORION_LD, SCORPIO, POSTGRESQL);
	// postgresql does not read through the entities cache, so it gets no redis run
	private static final List<String> NGSI_LD_RUNS = List.of(ORION_LD, SCORPIO, ORION_LD + REDIS_SUFFIX,
			SCORPIO + REDIS_SUFFIX);
	private static final List<String> ALL_RUNS = Stream.concat(NGSI_LD_RUNS.stream(), Stream.of(POSTGRESQL))
			.toList();

	private static final String PRODUCT_OFFERING_PATH = "/productOffering";
	private static final String PRODUCT_SPECIFICATION_PATH = "/productSpecification";
	private static final String FIXTURE_DIRECTORY = "/access-policy/";
	private static final String UC1_TERM_FIXTURE = "uc1-product-offering-term.json";
	private static final String UC6_POLICY_FIXTURE = "uc6-policy.json";
	// a lossy shape pinned for one run and level wins over the one shared by every level and cache of a storage
	private static final String OBSERVED_FORMAT = "%s-%s-observed-%s.json";
	private static final String RUN_LEVEL_FORMAT = "%s-%s";
	private static final String TERM_SCHEMA_LOCATION =
			"http://localhost:3000/schemas/product-offering-term-policy-schema.json";

	private static final String ACCESS_POLICY = "accessPolicy";
	private static final String CONTRACT_POLICY = "contractPolicy";
	private static final String OFFERING_TERM_POINTER = "/productOfferingTerm/0/";
	private static final String PRODUCT_SPEC_VALUE_POINTER =
			"/productSpecCharacteristic/0/productSpecCharacteristicValue/0/value";
	private static final String CHARACTERISTIC_ID = "visibility";
	private static final String CHARACTERISTIC_NAME = "Who may see products built from this specification";
	private static final String AUTHORIZATION_POLICY_VALUE_TYPE = "authorizationPolicy";
	private static final String PRODUCT_CONTAINER = "productSpecCharacteristicValue";
	// the ServiceSpecification's container name, the documented authoring mistake on a product specification
	private static final String SERVICE_CONTAINER = "characteristicValueSpecification";
	// observed: Micronaut answers "Invalid JSON" with ValidatingDeserializer's message and Jackson's reference chain;
	// the rejected key names do not reach the body
	private static final String UNKNOWN_PROPERTY_REJECTION =
			"No additional properties are allowed without a @schemaLocation";
	private static final String OFFERING_TERM_CHAIN = "ProductOfferingCreateVO[\\\"productOfferingTerm\\\"]";
	private static final String PRODUCT_SPEC_CHARACTERISTIC_CHAIN =
			"ProductSpecificationCreateVO[\\\"productSpecCharacteristic\\\"]";

	private final HttpClient httpClient;
	private final EntityReadRepository entityReadRepository;
	private final CacheManager<Object> cacheManager;
	private final TestEntityCleaner testEntityCleaner;
	private final Environment environment;

	public ProductOfferingPolicyRoundTripIT(@Client("/") HttpClient httpClient, EntitiesApiClient entitiesApiClient,
											ObjectMapper objectMapper, GeneralProperties generalProperties,
											EntityReadRepository entityReadRepository,
											CacheManager<Object> cacheManager, TestEntityCleaner testEntityCleaner,
											Environment environment) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.httpClient = httpClient;
		this.entityReadRepository = entityReadRepository;
		this.cacheManager = cacheManager;
		this.testEntityCleaner = testEntityCleaner;
		this.environment = environment;
	}

	@Override
	protected String getEntityType() {
		return ProductOffering.TYPE_PRODUCT_OFFERING;
	}

	@BeforeEach
	public void cleanUpSpecifications() {
		testEntityCleaner.deleteAll(ProductSpecification.TYPE_PRODUCT_SPECIFICATION);
	}

	/** Where a stored policy is observed. */
	enum Level {
		API, STORE_READ, CACHED_READ
	}

	/** What a run did to the policy at one level. */
	enum Outcome {
		/** equal to the request subtree */
		PRESERVED,
		/** differs from the request, equal to the pinned observed-shape file */
		LOSSY,
		/** absent after the round trip */
		DROPPED,
		/** the POST answered the case's pinned 4xx */
		REJECTED
	}

	/** One policy location: its JSON pointer in request and response, and its domain accessor. */
	record Probe(String name, String pointer, Function<Object, Object> domainAccessor) {
	}

	/**
	 * One request and its pinned outcomes. {@code expected} holds, per level, the outcome per run key
	 * ({@code <storage>[+redis]}); a level without the current run is not checked. A rejection must answer
	 * {@code rejectedWith} with every {@code rejectedBecause} fragment in its body.
	 */
	record RoundTripCase(String name, String path, Class<?> domainClass, ObjectNode body, List<Probe> probes,
						 HttpStatus rejectedWith, List<String> rejectedBecause,
						 Map<Level, Map<String, Outcome>> expected) {
		@Override
		public String toString() {
			return name;
		}
	}

	static Stream<RoundTripCase> cases() {
		ObjectNode verbatimTerm = (ObjectNode) fixture(UC1_TERM_FIXTURE);
		ObjectNode termWithSchema = verbatimTerm.deepCopy();
		termWithSchema.put("@schemaLocation", TERM_SCHEMA_LOCATION);
		List<Probe> termProbes = List.of(termProbe(ACCESS_POLICY), termProbe(CONTRACT_POLICY));
		List<Probe> specProbes = List.of(new Probe("value", PRODUCT_SPEC_VALUE_POINTER,
				ProductOfferingPolicyRoundTripIT::productSpecValue));
		return Stream.of(
				// ValidatingDeserializer: no unknown properties without a @schemaLocation
				new RoundTripCase("OFFERING_TERM_VERBATIM", PRODUCT_OFFERING_PATH, ProductOffering.class,
						offering(verbatimTerm), termProbes, HttpStatus.BAD_REQUEST,
						List.of(UNKNOWN_PROPERTY_REJECTION, OFFERING_TERM_CHAIN), rejected()),
				new RoundTripCase("OFFERING_TERM_WITH_SCHEMA", PRODUCT_OFFERING_PATH, ProductOffering.class,
						offering(termWithSchema), termProbes, null, null,
						// suspected, not observed: scorpio may compact the single-element "permission" array
						stored(Outcome.PRESERVED, Map.of())),
				new RoundTripCase("PRODUCT_SPEC_CHARACTERISTIC_VALUE", PRODUCT_SPECIFICATION_PATH,
						ProductSpecification.class, specification(PRODUCT_CONTAINER), specProbes, null, null,
						stored(Outcome.PRESERVED, Map.of())),
				// the characteristic has a @schemaLocation property, so the unknown container is rejected
				new RoundTripCase("PRODUCT_SPEC_WRONG_CONTAINER", PRODUCT_SPECIFICATION_PATH,
						ProductSpecification.class, specification(SERVICE_CONTAINER), specProbes,
						HttpStatus.BAD_REQUEST, List.of(UNKNOWN_PROPERTY_REJECTION, PRODUCT_SPEC_CHARACTERISTIC_CHAIN),
						rejected()));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("cases")
	public void policyRoundTrip(RoundTripCase roundTripCase) throws Exception {
		String run = run();
		HttpResponse<String> created = callAndCatch(() -> httpClient.toBlocking()
				.exchange(HttpRequest.POST(roundTripCase.path(), roundTripCase.body().toString()), String.class));
		if (expected(roundTripCase, Level.API, run) == Outcome.REJECTED) {
			String error = created.getBody(String.class).orElse("");
			List<Executable> rejection = new ArrayList<>();
			rejection.add(() -> assertEquals(roundTripCase.rejectedWith(), created.getStatus(),
					roundTripCase + " should be rejected on " + run + ": " + error));
			roundTripCase.rejectedBecause().forEach(fragment -> rejection.add(() -> assertTrue(error.contains(fragment),
					roundTripCase + " should be rejected with \"" + fragment + "\" on " + run + ": " + error)));
			assertAll(rejection);
			return;
		}
		// the POST body is built from the client-side object, so only its id is used
		assertEquals(HttpStatus.CREATED, created.getStatus(),
				roundTripCase + " should be created on " + run + ": " + created.getBody(String.class));
		String id = objectMapper.readTree(created.body()).get("id").asString();

		HttpResponse<String> read = callAndCatch(() -> httpClient.toBlocking()
				.exchange(HttpRequest.GET(roundTripCase.path() + "/" + id), String.class));
		assertEquals(HttpStatus.OK, read.getStatus(), "The created entity should be readable.");
		JsonNode apiBody = objectMapper.readTree(read.body());
		// every probe at every level is reported, so one run shows all that needs pinning
		List<Executable> checks = new ArrayList<>(
				checkProbes(roundTripCase, Level.API, run, probe -> apiBody.at(probe.pointer())));

		URI entityId = URI.create(id);
		invalidateEntityCache(entityId);
		Object stored = entityReadRepository.get(entityId, roundTripCase.domainClass()).block();
		assertNotNull(stored, "The created entity should be readable through the port.");
		checks.addAll(checkProbes(roundTripCase, Level.STORE_READ, run, probe -> domainTree(probe, stored)));

		if (isNgsiLd()) {
			assertTrue(cacheManager.getCache(CommonConstants.ENTITIES_CACHE_NAME).get(entityId, EntityVO.class)
					.isPresent(), "The store read should have filled the entities cache.");
			Object cached = entityReadRepository.get(entityId, roundTripCase.domainClass()).block();
			assertNotNull(cached, "The cached entity should be readable through the port.");
			checks.addAll(checkProbes(roundTripCase, Level.CACHED_READ, run, probe -> domainTree(probe, cached)));
		}
		assertAll(checks);
	}

	private List<Executable> checkProbes(RoundTripCase roundTripCase, Level level, String run,
										 Function<Probe, JsonNode> actualAt) {
		Outcome expected = expected(roundTripCase, level, run);
		List<Executable> checks = new ArrayList<>();
		for (Probe probe : roundTripCase.probes()) {
			JsonNode requested = roundTripCase.body().at(probe.pointer());
			JsonNode actual = actualAt.apply(probe);
			Outcome observed = outcome(requested, actual);
			String context = String.format("%s %s at %s on %s", roundTripCase, probe.name(), level, run);
			checks.add(() -> assertEquals(expected, observed, context + ", observed: " + actual));
			if (observed == Outcome.LOSSY) {
				checks.add(() -> assertEquals(observedShape(roundTripCase, probe, level, run), actual,
						context + " should keep its pinned lossy shape."));
			}
		}
		return checks;
	}

	private JsonNode observedShape(RoundTripCase roundTripCase, Probe probe, Level level, String run) {
		String prefix = roundTripCase.name().toLowerCase().replace('_', '-');
		String runAndLevel = String.format(RUN_LEVEL_FORMAT, run, level.name().toLowerCase().replace('_', '-'));
		String specific = String.format(OBSERVED_FORMAT, prefix, probe.name(), runAndLevel);
		return hasFixture(specific)
				? fixture(specific)
				: fixture(String.format(OBSERVED_FORMAT, prefix, probe.name(), storage()));
	}

	private static Outcome outcome(JsonNode requested, JsonNode actual) {
		if (actual == null || actual.isMissingNode() || actual.isNull()) {
			return Outcome.DROPPED;
		}
		return requested.equals(actual) ? Outcome.PRESERVED : Outcome.LOSSY;
	}

	private JsonNode domainTree(Probe probe, Object domainObject) {
		Object value = probe.domainAccessor().apply(domainObject);
		return value == null ? null : objectMapper.valueToTree(value);
	}

	// the GET after the POST already filled the cache; the store read has to miss it
	private void invalidateEntityCache(URI entityId) {
		if (cacheManager.getCacheNames().contains(CommonConstants.ENTITIES_CACHE_NAME)) {
			cacheManager.getCache(CommonConstants.ENTITIES_CACHE_NAME).invalidate(entityId);
		}
	}

	private boolean isNgsiLd() {
		Set<String> active = environment.getActiveNames();
		return active.contains(ORION_LD) || active.contains(SCORPIO);
	}

	private String run() {
		return environment.getActiveNames().contains(REDIS) ? storage() + REDIS_SUFFIX : storage();
	}

	private String storage() {
		Set<String> active = environment.getActiveNames();
		return STORAGES.stream()
				.filter(active::contains)
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("No storage environment active: " + active));
	}

	private static Outcome expected(RoundTripCase roundTripCase, Level level, String run) {
		Outcome outcome = roundTripCase.expected().getOrDefault(level, Map.of()).get(run);
		if (outcome == null) {
			throw new IllegalStateException(String.format("%s pins no %s outcome for %s", roundTripCase, level, run));
		}
		return outcome;
	}

	private static Map<Level, Map<String, Outcome>> rejected() {
		return Map.of(Level.API, everywhere(ALL_RUNS, Outcome.REJECTED, Map.of()));
	}

	// the same outcome at every level a run reaches; deviating runs keep theirs at every level
	private static Map<Level, Map<String, Outcome>> stored(Outcome outcome, Map<String, Outcome> deviatingRuns) {
		Map<Level, Map<String, Outcome>> expected = new EnumMap<>(Level.class);
		expected.put(Level.API, everywhere(ALL_RUNS, outcome, deviatingRuns));
		expected.put(Level.STORE_READ, everywhere(ALL_RUNS, outcome, deviatingRuns));
		expected.put(Level.CACHED_READ, everywhere(NGSI_LD_RUNS, outcome, deviatingRuns));
		return expected;
	}

	private static Map<String, Outcome> everywhere(List<String> runs, Outcome outcome,
												   Map<String, Outcome> deviatingRuns) {
		return runs.stream()
				.collect(Collectors.toMap(Function.identity(), run -> deviatingRuns.getOrDefault(run, outcome)));
	}

	private static Probe termProbe(String policyName) {
		return new Probe(policyName, OFFERING_TERM_POINTER + policyName,
				offering -> termPolicy((ProductOffering) offering, policyName));
	}

	private static Object termPolicy(ProductOffering offering, String policyName) {
		List<ProductOfferingTerm> terms = offering.getProductOfferingTerm();
		if (terms == null || terms.isEmpty() || terms.getFirst().getAdditionalProperties() == null) {
			return null;
		}
		return terms.getFirst().getAdditionalProperties().stream()
				.filter(property -> policyName.equals(property.getName()))
				.findFirst()
				.map(UnmappedProperty::getValue)
				.orElse(null);
	}

	private static Object productSpecValue(Object specification) {
		List<ProductSpecificationCharacteristic> characteristics =
				((ProductSpecification) specification).getProductSpecCharacteristic();
		if (characteristics == null || characteristics.isEmpty()) {
			return null;
		}
		List<CharacteristicValueSpecification> values = characteristics.getFirst().getProductSpecCharacteristicValue();
		return values == null || values.isEmpty() ? null : values.getFirst().getTmfValue();
	}

	private static ObjectNode offering(ObjectNode term) {
		ObjectNode offering = JsonNodeFactory.instance.objectNode();
		offering.put("name", "AP-01 offering");
		offering.putArray("productOfferingTerm").add(term);
		return offering;
	}

	private static ObjectNode specification(String valueContainer) {
		ObjectNode value = JsonNodeFactory.instance.objectNode();
		value.put("isDefault", true);
		value.set("value", fixture(UC6_POLICY_FIXTURE));
		ObjectNode characteristic = JsonNodeFactory.instance.objectNode();
		characteristic.put("id", CHARACTERISTIC_ID);
		characteristic.put("name", CHARACTERISTIC_NAME);
		characteristic.put("valueType", AUTHORIZATION_POLICY_VALUE_TYPE);
		characteristic.putArray(valueContainer).add(value);
		ObjectNode specification = JsonNodeFactory.instance.objectNode();
		specification.put("name", "AP-01 specification");
		specification.putArray("productSpecCharacteristic").add(characteristic);
		return specification;
	}

	private static boolean hasFixture(String fileName) {
		return ProductOfferingPolicyRoundTripIT.class.getResource(FIXTURE_DIRECTORY + fileName) != null;
	}

	private static JsonNode fixture(String fileName) {
		try (InputStream fixture = ProductOfferingPolicyRoundTripIT.class
				.getResourceAsStream(FIXTURE_DIRECTORY + fileName)) {
			if (fixture == null) {
				throw new IllegalStateException("Missing fixture " + FIXTURE_DIRECTORY + fileName);
			}
			return new ObjectMapper().readTree(fixture);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
