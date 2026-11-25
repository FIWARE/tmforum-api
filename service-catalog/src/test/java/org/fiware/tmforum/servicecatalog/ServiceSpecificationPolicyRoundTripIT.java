package org.fiware.tmforum.servicecatalog;

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
import org.fiware.tmforum.service.CharacteristicSpecification;
import org.fiware.tmforum.service.CharacteristicValueSpecification;
import org.fiware.tmforum.servicecatalog.domain.ServiceSpecification;
import org.fiware.tmforum.storage.EntityReadRepository;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
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
 * AP-01: pins whether an ODRL policy on a service specification characteristic value survives POST -> storage -> GET,
 * and whether it is reachable on the domain object the storage port returns. Mirrors ProductOfferingPolicyRoundTripIT.
 * Subtrees compare by {@link JsonNode#equals}: key order is ignored, array order, value types and keys are not.
 * The outcomes below were observed on orion-ld, scorpio, postgresql, orion-ld+redis and scorpio+redis.
 */
@MicronautTest(packages = {"org.fiware.tmforum.servicecatalog"}, contextBuilder = FirstMatchConfigContextBuilder.class)
public class ServiceSpecificationPolicyRoundTripIT extends AbstractApiIT {

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

	private static final String SERVICE_SPECIFICATION_PATH = "/serviceSpecification";
	private static final String FIXTURE_DIRECTORY = "/access-policy/";
	private static final String UC6_SPECIFICATION_FIXTURE = "uc6-service-specification.json";
	// a lossy shape pinned for one run and level wins over the one shared by every level and cache of a storage
	private static final String OBSERVED_FORMAT = "%s-observed-%s.json";
	private static final String RUN_LEVEL_FORMAT = "%s-%s";

	private static final String SERVICE_CHARACTERISTIC = "specCharacteristic";
	private static final String SERVICE_CONTAINER = "characteristicValueSpecification";
	// the ProductSpecification's container names, the documented authoring mistake on a service specification
	private static final String PRODUCT_CHARACTERISTIC = "productSpecCharacteristic";
	private static final String PRODUCT_CONTAINER = "productSpecCharacteristicValue";
	private static final String VALUE_POINTER = "/specCharacteristic/0/characteristicValueSpecification/0/value";
	private static final String UNKNOWN_PROPERTY_REJECTION =
			"No additional properties are allowed without a @schemaLocation";
	// observed: a nested unknown key answers "Invalid JSON" with Jackson's reference chain, not the key name
	private static final String CHARACTERISTIC_CHAIN = "ServiceSpecificationCreateVO[\\\"specCharacteristic\\\"]";
	// observed: a top-level unknown key fails the schema validation, which names the key
	private static final String TOP_LEVEL_KEY_REJECTION =
			"Additional properties not allowed: [" + PRODUCT_CHARACTERISTIC + "]";

	private final HttpClient httpClient;
	private final EntityReadRepository entityReadRepository;
	private final CacheManager<Object> cacheManager;
	private final Environment environment;

	public ServiceSpecificationPolicyRoundTripIT(@Client("/") HttpClient httpClient,
												 EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper,
												 GeneralProperties generalProperties,
												 EntityReadRepository entityReadRepository,
												 CacheManager<Object> cacheManager, Environment environment) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.httpClient = httpClient;
		this.entityReadRepository = entityReadRepository;
		this.cacheManager = cacheManager;
		this.environment = environment;
	}

	@Override
	protected String getEntityType() {
		return ServiceSpecification.TYPE_SERVICE_SPECIFICATION;
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

	/**
	 * One request and its pinned outcomes. {@code expected} holds, per level, the outcome per run key
	 * ({@code <storage>[+redis]}); a level without the current run is not checked. A rejection must answer
	 * {@code rejectedWith} with every {@code rejectedBecause} fragment in its body.
	 */
	record RoundTripCase(String name, ObjectNode body, HttpStatus rejectedWith, List<String> rejectedBecause,
						 Map<Level, Map<String, Outcome>> expected) {
		@Override
		public String toString() {
			return name;
		}
	}

	static Stream<RoundTripCase> cases() {
		ObjectNode verbatim = (ObjectNode) fixture(UC6_SPECIFICATION_FIXTURE);
		ObjectNode productValueContainer = verbatim.deepCopy();
		rename(characteristic(productValueContainer), SERVICE_CONTAINER, PRODUCT_CONTAINER);
		ObjectNode productContainers = productValueContainer.deepCopy();
		rename(productContainers, SERVICE_CHARACTERISTIC, PRODUCT_CHARACTERISTIC);
		return Stream.of(
				new RoundTripCase("SERVICE_SPEC_CHARACTERISTIC_VALUE", verbatim, null, null,
						stored(Outcome.PRESERVED, Map.of())),
				// the characteristic has a @schemaLocation property, so the unknown value container is rejected
				new RoundTripCase("SERVICE_SPEC_WRONG_CONTAINER", productValueContainer, HttpStatus.BAD_REQUEST,
						List.of(UNKNOWN_PROPERTY_REJECTION, CHARACTERISTIC_CHAIN), rejected()),
				// the specification has a @schemaLocation property, so the unknown characteristic container is rejected
				new RoundTripCase("SERVICE_SPEC_WRONG_CHARACTERISTIC_CONTAINER", productContainers,
						HttpStatus.BAD_REQUEST, List.of(UNKNOWN_PROPERTY_REJECTION, TOP_LEVEL_KEY_REJECTION), rejected()));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("cases")
	public void policyRoundTrip(RoundTripCase roundTripCase) throws Exception {
		String run = run();
		HttpResponse<String> created = callAndCatch(() -> httpClient.toBlocking()
				.exchange(HttpRequest.POST(SERVICE_SPECIFICATION_PATH, roundTripCase.body().toString()), String.class));
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
				.exchange(HttpRequest.GET(SERVICE_SPECIFICATION_PATH + "/" + id), String.class));
		assertEquals(HttpStatus.OK, read.getStatus(), "The created entity should be readable.");
		JsonNode apiBody = objectMapper.readTree(read.body());
		// every level is reported, so one run shows all that needs pinning
		List<Executable> checks = new ArrayList<>();
		checks.addAll(check(roundTripCase, Level.API, run, apiBody.at(VALUE_POINTER)));

		URI entityId = URI.create(id);
		invalidateEntityCache(entityId);
		ServiceSpecification stored = entityReadRepository.get(entityId, ServiceSpecification.class).block();
		assertNotNull(stored, "The created entity should be readable through the port.");
		checks.addAll(check(roundTripCase, Level.STORE_READ, run, domainTree(stored)));

		if (isNgsiLd()) {
			assertTrue(cacheManager.getCache(CommonConstants.ENTITIES_CACHE_NAME).get(entityId, EntityVO.class)
					.isPresent(), "The store read should have filled the entities cache.");
			ServiceSpecification cached = entityReadRepository.get(entityId, ServiceSpecification.class).block();
			assertNotNull(cached, "The cached entity should be readable through the port.");
			checks.addAll(check(roundTripCase, Level.CACHED_READ, run, domainTree(cached)));
		}
		assertAll(checks);
	}

	private List<Executable> check(RoundTripCase roundTripCase, Level level, String run, JsonNode actual) {
		Outcome expected = expected(roundTripCase, level, run);
		Outcome observed = outcome(roundTripCase.body().at(VALUE_POINTER), actual);
		String context = String.format("%s at %s on %s", roundTripCase, level, run);
		List<Executable> checks = new ArrayList<>();
		checks.add(() -> assertEquals(expected, observed, context + ", observed: " + actual));
		if (observed == Outcome.LOSSY) {
			checks.add(() -> assertEquals(observedShape(roundTripCase, level, run), actual,
					context + " should keep its pinned lossy shape."));
		}
		return checks;
	}

	private JsonNode observedShape(RoundTripCase roundTripCase, Level level, String run) {
		String prefix = roundTripCase.name().toLowerCase().replace('_', '-');
		String runAndLevel = String.format(RUN_LEVEL_FORMAT, run, level.name().toLowerCase().replace('_', '-'));
		String specific = String.format(OBSERVED_FORMAT, prefix, runAndLevel);
		return hasFixture(specific)
				? fixture(specific)
				: fixture(String.format(OBSERVED_FORMAT, prefix, storage()));
	}

	private static Outcome outcome(JsonNode requested, JsonNode actual) {
		if (actual == null || actual.isMissingNode() || actual.isNull()) {
			return Outcome.DROPPED;
		}
		return requested.equals(actual) ? Outcome.PRESERVED : Outcome.LOSSY;
	}

	private JsonNode domainTree(ServiceSpecification specification) {
		Object value = specValue(specification);
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

	private static Object specValue(ServiceSpecification specification) {
		List<CharacteristicSpecification> characteristics = specification.getSpecCharacteristic();
		if (characteristics == null || characteristics.isEmpty()) {
			return null;
		}
		List<CharacteristicValueSpecification> values = characteristics.getFirst().getCharacteristicValueSpecification();
		return values == null || values.isEmpty() ? null : values.getFirst().getTmfValue();
	}

	private static ObjectNode characteristic(ObjectNode specification) {
		return (ObjectNode) specification.withArray(SERVICE_CHARACTERISTIC).get(0);
	}

	private static void rename(ObjectNode node, String from, String to) {
		node.set(to, node.remove(from));
	}

	private static boolean hasFixture(String fileName) {
		return ServiceSpecificationPolicyRoundTripIT.class.getResource(FIXTURE_DIRECTORY + fileName) != null;
	}

	private static JsonNode fixture(String fileName) {
		try (InputStream fixture = ServiceSpecificationPolicyRoundTripIT.class
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
