package org.fiware.tmforum.product;

import io.github.wistefan.mapping.UnmappedProperty;
import org.fiware.tmforum.accesspolicy.PolicyScope;
import org.fiware.tmforum.accesspolicy.ResolvedPolicies;
import org.fiware.tmforum.accesspolicy.UnreadablePolicy;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductOfferingPolicyCarrierTest {

	private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();
	private static final ProductOfferingPolicyCarrier CARRIER = new ProductOfferingPolicyCarrier(OBJECT_MAPPER);

	private static final String OFFERING_ID = "urn:ngsi-ld:product-offering:o";
	private static final String UC1_TERM_RESOURCE = "/access-policy/uc1-product-offering-term.json";
	private static final String UC1_ACCESS_UID = "https://mp-operation.org/policy/offering/k8s-eu/access";
	private static final String UC1_CONTRACT_UID = "https://mp-operation.org/policy/offering/k8s-eu/contract";
	private static final String OTHER_PROPERTY = "otherPolicy";
	private static final String FIRST_ACCESS = "productOfferingTerm[0].accessPolicy";
	private static final String FIRST_CONTRACT = "productOfferingTerm[0].contractPolicy";
	private static final String SECOND_ACCESS = "productOfferingTerm[1].accessPolicy";
	private static final String SCOPE_LESS_POLICY = """
			{"@context": {"odrl": "http://www.w3.org/ns/odrl/2/"},
			 "odrl:uid": "urn:policy:scope-less",
			 "odrl:permission": {"odrl:action": {"@id": "odrl:read"}}}""";
	private static final String UID_LESS_POLICY = """
			{"@context": {"odrl": "http://www.w3.org/ns/odrl/2/", "dspace": "https://w3id.org/dspace/2024/1/"},
			 "odrl:permission": {"dspace:scope": "catalog", "odrl:action": {"@id": "odrl:read"}}}""";
	private static final String STRING_VALUE = "not a policy";

	static Stream<Arguments> offerings() {
		JsonNode uc1Term = uc1Term();
		return Stream.of(
				Arguments.of("UC-1 term at catalog", offering(uc1PolicyTerm(uc1Term)), PolicyScope.CATALOG,
						List.of(UC1_ACCESS_UID), List.of()),
				Arguments.of("UC-1 term at contract negotiation", offering(uc1PolicyTerm(uc1Term)),
						PolicyScope.CONTRACT_NEGOTIATION, List.of(UC1_CONTRACT_UID), List.of()),
				Arguments.of("UC-1 policies as maps", offering(term(
								property(ProductOfferingPolicyCarrier.ACCESS_POLICY,
										OBJECT_MAPPER.convertValue(uc1Term.get(ProductOfferingPolicyCarrier.ACCESS_POLICY),
												Map.class)))),
						PolicyScope.CATALOG, List.of(UC1_ACCESS_UID), List.of()),
				Arguments.of("a scope-less term policy", offering(term(
								property(ProductOfferingPolicyCarrier.ACCESS_POLICY, OBJECT_MAPPER.readTree(SCOPE_LESS_POLICY)))),
						PolicyScope.CATALOG, List.of(), List.of()),
				Arguments.of("a policy without uid and a non-object value", offering(term(
								property(ProductOfferingPolicyCarrier.ACCESS_POLICY, OBJECT_MAPPER.readTree(UID_LESS_POLICY)),
								property(ProductOfferingPolicyCarrier.CONTRACT_POLICY, STRING_VALUE))),
						PolicyScope.CATALOG, List.of(), List.of(FIRST_ACCESS, FIRST_CONTRACT)),
				Arguments.of("an unreadable policy keeps its term index", offering(
								term(property(OTHER_PROPERTY, STRING_VALUE)),
								term(property(ProductOfferingPolicyCarrier.ACCESS_POLICY, STRING_VALUE))),
						PolicyScope.CATALOG, List.of(), List.of(SECOND_ACCESS)),
				Arguments.of("a term without policy keys", offering(term(property(OTHER_PROPERTY, STRING_VALUE))),
						PolicyScope.CATALOG, List.of(), List.of()),
				Arguments.of("no terms", new ProductOffering(OFFERING_ID), PolicyScope.CATALOG, List.of(), List.of()),
				Arguments.of("null terms, properties and property entries",
						offering(null, new ProductOfferingTerm(), term((UnmappedProperty) null)),
						PolicyScope.CATALOG, List.of(), List.of()));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("offerings")
	void resolvesTermPolicies(String description, ProductOffering offering, PolicyScope scope,
			List<String> expectedUids, List<String> expectedUnreadableLocations) {
		ResolvedPolicies resolved = CARRIER.resolve(offering, scope);

		assertEquals(expectedUids, List.copyOf(resolved.policies().keySet()), description);
		assertEquals(expectedUnreadableLocations,
				resolved.unreadable().stream().map(UnreadablePolicy::location).toList(), description);
	}

	private static ProductOffering offering(ProductOfferingTerm... terms) {
		ProductOffering offering = new ProductOffering(OFFERING_ID);
		offering.setProductOfferingTerm(Arrays.asList(terms));
		return offering;
	}

	private static ProductOfferingTerm term(UnmappedProperty... properties) {
		ProductOfferingTerm term = new ProductOfferingTerm();
		term.setAdditionalProperties(new ArrayList<>(Arrays.asList(properties)));
		return term;
	}

	private static UnmappedProperty property(String name, Object value) {
		return new UnmappedProperty(name, value);
	}

	private static ProductOfferingTerm uc1PolicyTerm(JsonNode uc1Term) {
		return term(
				property(ProductOfferingPolicyCarrier.ACCESS_POLICY,
						uc1Term.get(ProductOfferingPolicyCarrier.ACCESS_POLICY)),
				property(ProductOfferingPolicyCarrier.CONTRACT_POLICY,
						uc1Term.get(ProductOfferingPolicyCarrier.CONTRACT_POLICY)));
	}

	private static JsonNode uc1Term() {
		try (InputStream term = ProductOfferingPolicyCarrierTest.class.getResourceAsStream(UC1_TERM_RESOURCE)) {
			return OBJECT_MAPPER.readTree(term);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
