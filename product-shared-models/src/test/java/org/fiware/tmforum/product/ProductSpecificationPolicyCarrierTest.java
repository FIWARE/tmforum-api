package org.fiware.tmforum.product;

import org.fiware.tmforum.accesspolicy.PolicyScope;
import org.fiware.tmforum.accesspolicy.ResolvedPolicies;
import org.fiware.tmforum.accesspolicy.UnreadablePolicy;
import org.fiware.tmforum.service.CharacteristicPolicyValues;
import org.fiware.tmforum.service.CharacteristicValueSpecification;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductSpecificationPolicyCarrierTest {

	private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();
	private static final ProductSpecificationPolicyCarrier CARRIER =
			new ProductSpecificationPolicyCarrier(OBJECT_MAPPER);

	private static final String SPECIFICATION_ID = "urn:ngsi-ld:product-specification:s";
	private static final String UC6_POLICY_RESOURCE = "/access-policy/uc6-policy.json";
	private static final String UC6_UID = "https://mp-operation.org/policy/service/k8s-eu/visibility";
	private static final String OTHER_VALUE_TYPE = "string";
	private static final String FIRST_VALUE = "productSpecCharacteristic[0].productSpecCharacteristicValue[0].value";
	private static final String SECOND_VALUE = "productSpecCharacteristic[0].productSpecCharacteristicValue[1].value";
	private static final String SECOND_CHARACTERISTIC_VALUE =
			"productSpecCharacteristic[1].productSpecCharacteristicValue[0].value";
	private static final String SCOPE_LESS_POLICY = """
			{"@context": {"odrl": "http://www.w3.org/ns/odrl/2/"},
			 "odrl:uid": "urn:policy:scope-less",
			 "odrl:permission": {"odrl:action": {"@id": "odrl:read"}}}""";
	private static final String UID_LESS_POLICY = """
			{"@context": {"odrl": "http://www.w3.org/ns/odrl/2/", "dspace": "https://w3id.org/dspace/2024/1/"},
			 "odrl:permission": {"dspace:scope": "catalog", "odrl:action": {"@id": "odrl:read"}}}""";
	private static final String STRING_VALUE = "not a policy";

	static Stream<Arguments> specifications() {
		return Stream.of(
				Arguments.of("the UC-6 policy as a map",
						spec(characteristic(CharacteristicPolicyValues.AUTHORIZATION_POLICY_VALUE_TYPE,
								value(uc6PolicyMap()))),
						List.of(UC6_UID), List.of()),
				Arguments.of("the UC-6 policy as a JsonNode",
						spec(characteristic(CharacteristicPolicyValues.AUTHORIZATION_POLICY_VALUE_TYPE,
								value(uc6PolicyTree()))),
						List.of(UC6_UID), List.of()),
				Arguments.of("a scope-less permission",
						spec(characteristic(CharacteristicPolicyValues.AUTHORIZATION_POLICY_VALUE_TYPE,
								value(OBJECT_MAPPER.readTree(SCOPE_LESS_POLICY)))),
						List.of(), List.of()),
				Arguments.of("a string value and a policy without uid",
						spec(characteristic(CharacteristicPolicyValues.AUTHORIZATION_POLICY_VALUE_TYPE,
								value(STRING_VALUE), value(OBJECT_MAPPER.readTree(UID_LESS_POLICY)))),
						List.of(), List.of(FIRST_VALUE, SECOND_VALUE)),
				Arguments.of("a characteristic of another value type is ignored",
						spec(characteristic(OTHER_VALUE_TYPE, value(STRING_VALUE)),
								characteristic(CharacteristicPolicyValues.AUTHORIZATION_POLICY_VALUE_TYPE,
										value(uc6PolicyMap()))),
						List.of(UC6_UID), List.of()),
				Arguments.of("an unreadable policy keeps its characteristic index",
						spec(characteristic(OTHER_VALUE_TYPE, value(uc6PolicyMap())),
								characteristic(CharacteristicPolicyValues.AUTHORIZATION_POLICY_VALUE_TYPE,
										value(STRING_VALUE))),
						List.of(), List.of(SECOND_CHARACTERISTIC_VALUE)),
				Arguments.of("no characteristics", new ProductSpecification(SPECIFICATION_ID), List.of(), List.of()),
				Arguments.of("null characteristic and value entries",
						spec(null, characteristic(CharacteristicPolicyValues.AUTHORIZATION_POLICY_VALUE_TYPE,
								(CharacteristicValueSpecification) null)),
						List.of(), List.of()),
				Arguments.of("a characteristic without values",
						spec(characteristic(CharacteristicPolicyValues.AUTHORIZATION_POLICY_VALUE_TYPE,
								(CharacteristicValueSpecification[]) null)),
						List.of(), List.of()));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("specifications")
	void resolvesAtCatalogScope(String description, ProductSpecification specification, List<String> expectedUids,
			List<String> expectedUnreadableLocations) {
		ResolvedPolicies resolved = CARRIER.resolve(specification, PolicyScope.CATALOG);

		assertEquals(expectedUids, List.copyOf(resolved.policies().keySet()), description);
		assertEquals(expectedUnreadableLocations,
				resolved.unreadable().stream().map(UnreadablePolicy::location).toList(), description);
	}

	private static ProductSpecification spec(ProductSpecificationCharacteristic... characteristics) {
		ProductSpecification specification = new ProductSpecification(SPECIFICATION_ID);
		specification.setProductSpecCharacteristic(Arrays.asList(characteristics));
		return specification;
	}

	private static ProductSpecificationCharacteristic characteristic(String valueType,
			CharacteristicValueSpecification... values) {
		ProductSpecificationCharacteristic characteristic = new ProductSpecificationCharacteristic();
		characteristic.setAtValueType(valueType);
		characteristic.setProductSpecCharacteristicValue(values == null ? null : Arrays.asList(values));
		return characteristic;
	}

	private static CharacteristicValueSpecification value(Object tmfValue) {
		CharacteristicValueSpecification value = new CharacteristicValueSpecification();
		value.setTmfValue(tmfValue);
		return value;
	}

	private static JsonNode uc6PolicyTree() {
		try (InputStream policy = ProductSpecificationPolicyCarrierTest.class.getResourceAsStream(
				UC6_POLICY_RESOURCE)) {
			return OBJECT_MAPPER.readTree(policy);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static Map<?, ?> uc6PolicyMap() {
		return OBJECT_MAPPER.convertValue(uc6PolicyTree(), Map.class);
	}
}
