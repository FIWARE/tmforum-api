package org.fiware.tmforum.accesspolicy;

import com.nimbusds.jose.util.JSONObjectUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.text.ParseException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.fiware.tmforum.accesspolicy.PolicyScope.CATALOG;
import static org.fiware.tmforum.accesspolicy.PolicyScope.CONTRACT_NEGOTIATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests scope narrowing, uid keying and the fail-safe stance of {@link ScopedPolicyResolution} on the policy shapes of
 * POLICY_EXAMPLES.md and on every malformed or uninterpretable variant.
 */
class ScopedPolicyResolutionTest {

	private static final String UID = "https://mp-operation.org/policy/offering/k8s-eu/access";
	private static final String OTHER_UID = "https://mp-operation.org/policy/offering/k8s-eu/contract";
	private static final String LOCATION = "productOfferingTerm[0].accessPolicy";
	private static final String OTHER_LOCATION = "productOfferingTerm[1].contractPolicy";
	private static final String CONFLICT_REASON = "conflicting policies share odrl:uid";
	private static final String UC1_CONTEXT = """
			"@context": ["http://www.w3.org/ns/odrl.jsonld",
			  {"vc": "https://w3id.org/fiware/dsc/vc#", "dspace": "https://w3id.org/dspace/2024/1/"}]""";
	private static final String UC6_CONTEXT = """
			"@context": {"odrl": "http://www.w3.org/ns/odrl/2/", "vc": "https://w3id.org/fiware/dsc/vc#",
			  "dspace": "https://w3id.org/dspace/2024/1/"}""";
	private static final String COUNTRY_CONSTRAINT = """
			{"leftOperand": "vc:country", "operator": "odrl:isAnyOf",
			 "rightOperand": [{"@value": "DE", "@type": "xsd:string"}, {"@value": "AT", "@type": "xsd:string"}]}""";
	private static final String CATALOG_RULE = """
			{"action": "read", "dspace:scope": "catalog", "constraint": %s}""".formatted(COUNTRY_CONSTRAINT);
	private static final String CONTRACT_RULE = """
			{"action": "use", "dspace:scope": "contract.negotiation", "constraint": %s}""".formatted(COUNTRY_CONSTRAINT);
	private static final String TRANSFER_RULE = """
			{"action": "use", "dspace:scope": "transfer.process", "constraint": %s}""".formatted(COUNTRY_CONSTRAINT);
	private static final String SCOPELESS_RULE = """
			{"action": "use", "constraint": %s}""".formatted(COUNTRY_CONSTRAINT);

	/** A UC-1 shaped policy: odrl.jsonld plus inline prefixes, bare ODRL terms, {@link #UID}. */
	private static String uc1(String members) {
		return """
				{%s, "@type": "Offer", "odrl:uid": "%s", "assigner": "did:web:mp-operations.org", %s}"""
				.formatted(UC1_CONTEXT, UID, members);
	}

	private static String uc1Permissions(String... rules) {
		return uc1("\"permission\": [%s]".formatted(String.join(", ", rules)));
	}

	/** A UC-6 shaped policy: inline prefixes only, prefixed keys, a single unwrapped odrl:permission. */
	private static String uc6(String scopeMember) {
		return """
				{%s, "@id": "%s", "odrl:uid": "%s", "@type": "odrl:Policy",
				 "odrl:permission": {"odrl:assigner": {"@id": "did:web:mp-operations.org"}, %s
				   "odrl:action": {"@id": "odrl:read"},
				   "odrl:constraint": {"@type": "odrl:Constraint", "odrl:leftOperand": "vc:country",
				     "odrl:operator": {"@id": "odrl:isAnyOf"}, "odrl:rightOperand": ["DE", "AT"]}}}"""
				.formatted(UC6_CONTEXT, UID, UID, scopeMember);
	}

	private static Map<String, Object> json(String json) {
		try {
			return JSONObjectUtils.parse(json);
		} catch (ParseException e) {
			throw new IllegalArgumentException("Invalid fixture: " + json, e);
		}
	}

	private static ResolvedPolicies resolve(PolicyScope scope, Object... values) {
		List<CarriedPolicy> carried = new ArrayList<>();
		IntStream.range(0, values.length).forEach(i -> carried.add(new CarriedPolicy("policy[" + i + "]",
				values[i] instanceof String text && text.startsWith("{") ? json(text) : values[i])));
		return ScopedPolicyResolution.resolve(carried, scope);
	}

	private static Stream<Arguments> wellFormed() {
		String mixed = uc1Permissions(CATALOG_RULE, SCOPELESS_RULE);
		String twoKinds = uc1("\"permission\": [%s], \"prohibition\": [%s]".formatted(CATALOG_RULE, CONTRACT_RULE));
		return Stream.of(
				Arguments.of("UC-1 access policy at catalog", uc1Permissions(CATALOG_RULE), CATALOG,
						uc1Permissions(CATALOG_RULE)),
				Arguments.of("UC-1 access policy at contract", uc1Permissions(CATALOG_RULE), CONTRACT_NEGOTIATION, null),
				Arguments.of("UC-1 contract policy at contract", uc1Permissions(CONTRACT_RULE), CONTRACT_NEGOTIATION,
						uc1Permissions(CONTRACT_RULE)),
				Arguments.of("UC-1 contract policy at catalog", uc1Permissions(CONTRACT_RULE), CATALOG, null),
				Arguments.of("UC-6 single permission at catalog", uc6("\"dspace:scope\": \"catalog\","), CATALOG,
						uc6("\"dspace:scope\": \"catalog\",")),
				Arguments.of("UC-6 single permission at contract", uc6("\"dspace:scope\": \"catalog\","),
						CONTRACT_NEGOTIATION, null),
				Arguments.of("UC-6 scope-less at catalog", uc6(""), CATALOG, null),
				Arguments.of("UC-6 scope-less at contract", uc6(""), CONTRACT_NEGOTIATION, null),
				Arguments.of("transfer.process is ignored at catalog", uc1Permissions(TRANSFER_RULE), CATALOG, null),
				Arguments.of("transfer.process is ignored at contract", uc1Permissions(TRANSFER_RULE),
						CONTRACT_NEGOTIATION, null),
				Arguments.of("scope-less at catalog", uc1Permissions(SCOPELESS_RULE), CATALOG, null),
				Arguments.of("scope-less at contract", uc1Permissions(SCOPELESS_RULE), CONTRACT_NEGOTIATION, null),
				Arguments.of("mixed keeps only the scoped rule", mixed, CATALOG, uc1Permissions(CATALOG_RULE)),
				Arguments.of("mixed at the other scope", mixed, CONTRACT_NEGOTIATION, null),
				Arguments.of("an emptied rule key is removed", twoKinds, CATALOG, uc1Permissions(CATALOG_RULE)),
				Arguments.of("the other rule key at its scope", twoKinds, CONTRACT_NEGOTIATION,
						uc1("\"prohibition\": [%s]".formatted(CONTRACT_RULE))),
				Arguments.of("scope as @value", uc6("\"dspace:scope\": {\"@value\": \"catalog\"},"), CATALOG,
						uc6("\"dspace:scope\": {\"@value\": \"catalog\"},")),
				Arguments.of("scope as a list", uc6("\"dspace:scope\": [\"catalog\"],"), CATALOG,
						uc6("\"dspace:scope\": [\"catalog\"],")),
				Arguments.of("scope as a list of @value", uc6("\"dspace:scope\": [{\"@value\": \"catalog\"}],"),
						CATALOG, uc6("\"dspace:scope\": [{\"@value\": \"catalog\"}],")),
				Arguments.of("scope under its full IRI",
						uc6("\"https://w3id.org/dspace/2024/1/scope\": \"catalog\","), CATALOG,
						uc6("\"https://w3id.org/dspace/2024/1/scope\": \"catalog\",")),
				Arguments.of("a rule with @id and @type",
						uc1Permissions("{\"@id\": \"urn:rule:1\", \"@type\": \"Permission\", \"action\": \"read\", "
								+ "\"dspace:scope\": \"catalog\"}"), CATALOG,
						uc1Permissions("{\"@id\": \"urn:rule:1\", \"@type\": \"Permission\", \"action\": \"read\", "
								+ "\"dspace:scope\": \"catalog\"}")),
				Arguments.of("UC-2 shape with the DSP context", """
								{"@context": ["http://www.w3.org/ns/odrl.jsonld", "https://w3id.org/dspace/2024/1/context.json"],
								 "@type": "Offer", "odrl:uid": "%s", "assigner": "did:web:mp-operations.org",
								 "permission": [{"action": "use", "dspace:scope": "contract.negotiation",
								   "constraint": {"leftOperand": "dspace:membershipType", "operator": "eq",
								     "rightOperand": "FullMember"}}]}""".formatted(UID), CONTRACT_NEGOTIATION,
						"""
								{"@context": ["http://www.w3.org/ns/odrl.jsonld", "https://w3id.org/dspace/2024/1/context.json"],
								 "@type": "Offer", "odrl:uid": "%s", "assigner": "did:web:mp-operations.org",
								 "permission": [{"action": "use", "dspace:scope": "contract.negotiation",
								   "constraint": {"leftOperand": "dspace:membershipType", "operator": "eq",
								     "rightOperand": "FullMember"}}]}""".formatted(UID)));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("wellFormed")
	void wellFormedPoliciesNarrowToTheScope(String name, String policy, PolicyScope scope, String expected) {
		ResolvedPolicies resolved = resolve(scope, policy);

		assertEquals(List.of(), resolved.unreadable());
		assertEquals(expected == null ? Map.of() : Map.of(UID, json(expected)), resolved.policies());
	}

	@ParameterizedTest
	@EnumSource(PolicyScope.class)
	void aBareUidUnderTheOdrlContextIsTheUid(PolicyScope scope) {
		String policy = """
				{"@context": ["http://www.w3.org/ns/odrl.jsonld", {"dspace": "https://w3id.org/dspace/2024/1/"}],
				 "uid": "%s", "permission": [%s, %s]}""".formatted(UID, CATALOG_RULE, CONTRACT_RULE);

		ResolvedPolicies resolved = resolve(scope, policy);

		assertEquals(List.of(), resolved.unreadable());
		assertEquals(Set.of(UID), resolved.policies().keySet());
	}

	private static Map<String, Object> throwingMap() {
		return new AbstractMap<>() {
			@Override
			public Set<Entry<String, Object>> entrySet() {
				throw new IllegalStateException("broken tree");
			}
		};
	}

	private static Stream<Arguments> unreadable() {
		String dspace2025 = """
				{"@context": {"odrl": "http://www.w3.org/ns/odrl/2/", "dspace": "https://w3id.org/dspace/2025/1/"},
				 "odrl:uid": "%s", "odrl:permission": {"odrl:action": "read", "dspace:scope": "catalog"}}"""
				.formatted(UID);
		return Stream.of(
				// shape
				Arguments.of("a string", "not a policy"),
				Arguments.of("a list", List.of(Map.of())),
				Arguments.of("a null value", null),
				Arguments.of("a broken map", throwingMap()),
				Arguments.of("no uid", """
						{"@context": "http://www.w3.org/ns/odrl.jsonld", "permission": [%s]}""".formatted(CATALOG_RULE)),
				Arguments.of("a uid of two values", uc1Permissions(CATALOG_RULE)
						.replace("\"odrl:uid\": \"" + UID + "\"", "\"odrl:uid\": [\"urn:a\", \"urn:b\"]")),
				Arguments.of("a rule value that is a string", uc1("\"permission\": \"urn:rule:1\"")),
				Arguments.of("a rule list holding a string", uc1Permissions(CATALOG_RULE, "\"urn:rule:1\"")),
				Arguments.of("a scope of two values", uc1Permissions(
						"{\"action\": \"read\", \"dspace:scope\": [\"catalog\", \"contract.negotiation\"]}")),
				Arguments.of("a scope as @id", uc1Permissions(
						"{\"action\": \"read\", \"dspace:scope\": {\"@id\": \"catalog\"}}")),
				Arguments.of("a capitalised scope value",
						uc1Permissions(CATALOG_RULE.replace("\"catalog\"", "\"Catalog\""))),
				Arguments.of("a misspelt scope value",
						uc1Permissions(CATALOG_RULE.replace("\"catalog\"", "\"catalogue\""))),
				Arguments.of("an unknown scope value at the other scope", uc1Permissions(CATALOG_RULE,
						CONTRACT_RULE.replace("\"contract.negotiation\"", "\"contract-negotiation\""))),
				Arguments.of("a numeric scope at the other scope",
						uc1Permissions(CATALOG_RULE, CONTRACT_RULE.replace("\"contract.negotiation\"", "42"))),
				// contexts and keys
				Arguments.of("an untrusted remote context", uc1Permissions(CATALOG_RULE)
						.replace("\"http://www.w3.org/ns/odrl.jsonld\"", "\"https://example.org/context.jsonld\"")),
				Arguments.of("an @vocab context", uc1Permissions(CATALOG_RULE)
						.replace("{\"vc\"", "{\"@vocab\": \"https://example.org/\", \"vc\"")),
				Arguments.of("a bare scope under only the DSP context", """
						{"@context": ["https://w3id.org/dspace/2024/1/context.json"], "odrl:uid": "%s",
						 "odrl:permission": [{"odrl:action": "read", "scope": "catalog"}]}""".formatted(UID)),
				Arguments.of("a bare scope under the ODRL context",
						uc1Permissions("{\"action\": \"read\", \"scope\": \"catalog\"}")),
				Arguments.of("an unknown bare key on the policy", uc1("\"visibility\": \"partners\", "
						+ "\"permission\": [%s]".formatted(CATALOG_RULE))),
				Arguments.of("bare ODRL terms without the ODRL context", uc6("\"dspace:scope\": \"catalog\",")
						.replace("\"odrl:action\"", "\"action\"")),
				// keywords
				Arguments.of("rules under @nest", uc1("\"@nest\": {\"permission\": [%s]}".formatted(CATALOG_RULE))),
				Arguments.of("rules under @graph", uc1("\"@graph\": [{\"permission\": [%s]}]".formatted(CATALOG_RULE))),
				Arguments.of("a rule value wrapped in @set", uc1("\"permission\": {\"@set\": [%s]}"
						.formatted(CATALOG_RULE))),
				Arguments.of("an @id-only rule", uc1Permissions(CATALOG_RULE, "{\"@id\": \"urn:rule:1\"}")),
				Arguments.of("a rule-scoped @context", uc1Permissions(CATALOG_RULE.replace("{\"action\"",
						"{\"@context\": {\"x\": \"https://example.org/x\"}, \"action\""))),
				// misplaced scope
				Arguments.of("a policy-level scope", uc1("\"dspace:scope\": \"catalog\", \"permission\": [%s]"
						.formatted(SCOPELESS_RULE))),
				Arguments.of("scope as a leftOperand", uc1Permissions(scopeConstraint("\"leftOperand\"",
						"\"dspace:scope\""))),
				Arguments.of("scope as an @id leftOperand", uc1Permissions(scopeConstraint("\"leftOperand\"",
						"{\"@id\": \"dspace:scope\"}"))),
				Arguments.of("scope as a bare leftOperand", uc1Permissions(scopeConstraint("\"leftOperand\"",
						"\"scope\""))),
				Arguments.of("scope as an odrl:leftOperand", uc1Permissions(scopeConstraint("\"odrl:leftOperand\"",
						"\"dspace:scope\""))),
				Arguments.of("a leftOperand of an unreadable shape", uc1Permissions(scopeConstraint("\"leftOperand\"",
						"{\"name\": \"scope\"}"))),
				Arguments.of("a scope on a duty", uc1Permissions("{\"action\": \"use\", \"duty\": "
						+ "[{\"action\": \"compensate\", \"dspace:scope\": \"catalog\"}]}")),
				Arguments.of("a scope inside a constraint", uc1Permissions("{\"action\": \"read\", \"constraint\": "
						+ "{\"dspace:scope\": \"catalog\", \"leftOperand\": \"vc:country\", \"operator\": \"eq\", "
						+ "\"rightOperand\": \"DE\"}}")),
				// wrong IRI
				Arguments.of("odrl:scope", uc1Permissions("{\"action\": \"read\", \"odrl:scope\": \"catalog\"}")),
				Arguments.of("a dspace prefix bound to 2025/1", dspace2025),
				Arguments.of("dspace:scope next to its full IRI", uc1Permissions("{\"action\": \"read\", "
						+ "\"dspace:scope\": \"catalog\", \"https://w3id.org/dspace/2024/1/scope\": \"catalog\"}")),
				// inherited rules live in the parent policy
				Arguments.of("a bare inheritFrom with own rules",
						uc1("\"inheritFrom\": \"urn:p:parent\", \"permission\": [%s]".formatted(SCOPELESS_RULE))),
				Arguments.of("a bare inheritFrom without own rules", uc1("\"inheritFrom\": \"urn:p:parent\"")),
				Arguments.of("an odrl:inheritFrom in the UC-6 shape",
						uc6("").replace("\"odrl:permission\"", "\"odrl:inheritFrom\": {\"@id\": \"urn:p:parent\"}, "
								+ "\"odrl:permission\"")),
				Arguments.of("inheritFrom under its full IRI",
						uc1("\"http://www.w3.org/ns/odrl/2/inheritFrom\": \"urn:p:parent\"")));
	}

	private static String scopeConstraint(String leftOperandKey, String leftOperand) {
		return """
				{"action": "read", "dspace:scope": "catalog",
				 "constraint": {%s: %s, "operator": "eq", "rightOperand": "catalog"}}"""
				.formatted(leftOperandKey, leftOperand);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("unreadable")
	void uninterpretablePoliciesAreUnreadableAtEveryScope(String name, Object policy) {
		for (PolicyScope scope : PolicyScope.values()) {
			ResolvedPolicies resolved = resolve(scope, policy);

			assertEquals(Map.of(), resolved.policies(), scope.value());
			assertEquals(1, resolved.unreadable().size(), scope.value());
			UnreadablePolicy unreadable = resolved.unreadable().getFirst();
			assertEquals("policy[0]", unreadable.location());
			assertNotNull(unreadable.reason());
			assertFalse(resolved.isEmpty(), "An unreadable policy must not look like an ungoverned entity.");
		}
	}

	@Test
	void anUnreadablePolicyKeepsItsUid() {
		ResolvedPolicies resolved = resolve(CATALOG, uc1Permissions("{\"action\": \"read\", \"odrl:scope\": \"catalog\"}"));

		assertEquals(UID, resolved.unreadable().getFirst().policyUid());
	}

	@ParameterizedTest
	@EnumSource(PolicyScope.class)
	void equalDuplicatesCollapse(PolicyScope scope) {
		String policy = uc1Permissions(CATALOG_RULE, CONTRACT_RULE);

		ResolvedPolicies resolved = resolve(scope, policy, policy);

		assertEquals(List.of(), resolved.unreadable());
		assertEquals(Set.of(UID), resolved.policies().keySet());
	}

	@ParameterizedTest
	@EnumSource(PolicyScope.class)
	void conflictingDuplicatesFailAtEveryScope(PolicyScope scope) {
		List<CarriedPolicy> carried = List.of(
				new CarriedPolicy(LOCATION, json(uc1Permissions(CATALOG_RULE))),
				new CarriedPolicy(OTHER_LOCATION, json(uc1Permissions(CONTRACT_RULE))));

		ResolvedPolicies resolved = ScopedPolicyResolution.resolve(carried, scope);

		assertEquals(Map.of(), resolved.policies());
		assertEquals(List.of(new UnreadablePolicy(LOCATION + ", " + OTHER_LOCATION, UID, CONFLICT_REASON)),
				resolved.unreadable());
	}

	@Test
	void distinctUidsKeepTheCarriedOrder() {
		String other = uc1Permissions(CATALOG_RULE).replace(UID, OTHER_UID);

		ResolvedPolicies resolved = resolve(CATALOG, other, uc1Permissions(CATALOG_RULE));

		assertEquals(List.of(OTHER_UID, UID), List.copyOf(resolved.policies().keySet()));
	}

	@Test
	void aReadablePolicyNextToAnUnreadableOneStillResolves() {
		ResolvedPolicies resolved = resolve(CATALOG, uc1Permissions(CATALOG_RULE), "not a policy");

		assertEquals(Set.of(UID), resolved.policies().keySet());
		assertEquals(1, resolved.unreadable().size());
	}

	@Test
	void emptiness() {
		assertTrue(ResolvedPolicies.NONE.isEmpty());
		assertTrue(resolve(CATALOG).isEmpty());
		assertTrue(resolve(CATALOG, uc1Permissions(SCOPELESS_RULE)).isEmpty());
		assertFalse(resolve(CATALOG, (Object) null).isEmpty());
	}

	@Test
	void theResultIsUnmodifiable() {
		ResolvedPolicies resolved = resolve(CATALOG, uc1Permissions(CATALOG_RULE));

		assertTrue(isUnmodifiable(() -> resolved.policies().clear()));
		assertTrue(isUnmodifiable(() -> resolved.policies().get(UID).clear()));
		assertTrue(isUnmodifiable(() -> resolved.unreadable().clear()));
	}

	private static boolean isUnmodifiable(Runnable mutation) {
		try {
			mutation.run();
			return false;
		} catch (UnsupportedOperationException e) {
			return true;
		}
	}
}
