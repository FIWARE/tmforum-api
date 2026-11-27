package org.fiware.tmforum.partyrole;

import io.github.wistefan.mapping.UnmappedProperty;
import io.micronaut.context.ApplicationContext;
import org.fiware.tmforum.accesspolicy.AccessPolicyProperties;
import org.fiware.tmforum.accesspolicy.PolicyCarriers;
import org.fiware.tmforum.accesspolicy.PolicyScope;
import org.fiware.tmforum.accesspolicy.ResolvedPolicies;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.product.ProductOffering;
import org.fiware.tmforum.product.ProductOfferingPolicyCarrier;
import org.fiware.tmforum.product.ProductOfferingTerm;
import org.fiware.tmforum.product.ProductSpecification;
import org.fiware.tmforum.product.ProductSpecificationCharacteristic;
import org.fiware.tmforum.service.CharacteristicPolicyValues;
import org.fiware.tmforum.service.CharacteristicSpecification;
import org.fiware.tmforum.service.CharacteristicValueSpecification;
import org.fiware.tmforum.servicecatalog.domain.ServiceSpecification;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Starts this module's context with access policies enabled, so every policy carrier runs against the module's
 * real classpath (the carried types' closure must be present, see IMPLEMENTATION_PLAN.md of ticket 186).
 */
class PolicyCarriersStartupTest {

	private static final String CACHE_ENVIRONMENT = "in-memory";
	private static final String POLICY_UID = "urn:policy:startup";
	private static final String POLICY = """
			{"@context": {"odrl": "http://www.w3.org/ns/odrl/2/", "dspace": "https://w3id.org/dspace/2024/1/"},
			 "odrl:uid": "%s",
			 "odrl:permission": {"dspace:scope": "catalog", "odrl:action": {"@id": "odrl:read"}}}""".formatted(POLICY_UID);
	private static final String JWKS_URI = "http://localhost/jwks";
	private static final String ISSUER = "https://issuer.example.org";
	private static final String PDP_URL = "http://pdp.example.org:8080";
	private static final String OFFERING_ID = "urn:ngsi-ld:product-offering:startup";
	private static final String PRODUCT_SPECIFICATION_ID = "urn:ngsi-ld:product-specification:startup";
	private static final String SERVICE_SPECIFICATION_ID = "urn:ngsi-ld:service-specification:startup";
	private static final Map<String, Object> ENABLED = Map.of(
			AccessPolicyProperties.ENABLED_PROPERTY, "true",
			AccessPolicyProperties.PREFIX + ".jwt.jwks-uri", JWKS_URI,
			AccessPolicyProperties.PREFIX + ".jwt.issuer", ISSUER,
			AccessPolicyProperties.PREFIX + ".pdp.url", PDP_URL);

	private static ApplicationContext context;

	@BeforeAll
	static void startContext() {
		context = new FirstMatchConfigContextBuilder().properties(ENABLED).environments(CACHE_ENVIRONMENT).start();
	}

	@AfterAll
	static void stopContext() {
		context.close();
	}

	static Stream<Arguments> governedEntities() {
		Map<?, ?> policy = JsonMapper.builder().build().readValue(POLICY, Map.class);
		return Stream.of(
				Arguments.of(ProductOffering.class, productOffering(policy)),
				Arguments.of(ProductSpecification.class, productSpecification(policy)),
				Arguments.of(ServiceSpecification.class, serviceSpecification(policy)));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("governedEntities")
	void resolvesEveryGovernedType(Class<?> type, Object entity) {
		PolicyCarriers carriers = context.getBean(PolicyCarriers.class);

		assertTrue(carriers.isGoverned(type), type.getSimpleName() + " must be governed.");
		ResolvedPolicies resolved = carriers.resolve(entity, PolicyScope.CATALOG);
		assertEquals(List.of(POLICY_UID), List.copyOf(resolved.policies().keySet()));
		assertEquals(List.of(), resolved.unreadable());
	}

	private static ProductOffering productOffering(Object policy) {
		ProductOfferingTerm term = new ProductOfferingTerm();
		term.setAdditionalProperties(List.of(new UnmappedProperty(ProductOfferingPolicyCarrier.ACCESS_POLICY, policy)));
		ProductOffering offering = new ProductOffering(OFFERING_ID);
		offering.setProductOfferingTerm(List.of(term));
		return offering;
	}

	private static ProductSpecification productSpecification(Object policy) {
		ProductSpecificationCharacteristic characteristic = new ProductSpecificationCharacteristic();
		characteristic.setAtValueType(CharacteristicPolicyValues.AUTHORIZATION_POLICY_VALUE_TYPE);
		characteristic.setProductSpecCharacteristicValue(List.of(value(policy)));
		ProductSpecification specification = new ProductSpecification(PRODUCT_SPECIFICATION_ID);
		specification.setProductSpecCharacteristic(List.of(characteristic));
		return specification;
	}

	private static ServiceSpecification serviceSpecification(Object policy) {
		CharacteristicSpecification characteristic = new CharacteristicSpecification();
		characteristic.setValueType(CharacteristicPolicyValues.AUTHORIZATION_POLICY_VALUE_TYPE);
		characteristic.setCharacteristicValueSpecification(List.of(value(policy)));
		ServiceSpecification specification = new ServiceSpecification(SERVICE_SPECIFICATION_ID);
		specification.setSpecCharacteristic(List.of(characteristic));
		return specification;
	}

	private static CharacteristicValueSpecification value(Object policy) {
		CharacteristicValueSpecification value = new CharacteristicValueSpecification();
		value.setTmfValue(policy);
		return value;
	}
}
