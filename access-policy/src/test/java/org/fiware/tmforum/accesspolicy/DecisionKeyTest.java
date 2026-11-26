package org.fiware.tmforum.accesspolicy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the decision cache key: every input that can change a verdict must change the key, and nothing else may.
 */
class DecisionKeyTest {

	private static final String SHA_256_HEX_PATTERN = "[0-9a-f]{64}";
	private static final String UID = "urn:policy:1";
	private static final String SCOPE = "catalog";
	private static final String ACTION = "read";
	private static final String IDENTITY = "did:web:consumer.example";
	private static final Instant EXPIRES_AT = Instant.parse("2026-11-26T12:00:00Z");
	private static final Map<String, Object> CLAIMS = Map.of("verifiableCredential",
			List.of(Map.of("type", List.of("VerifiableCredential", "OperatorCredential"))));
	private static final Map<String, Object> SUBJECT_ONLY_POLICY = Map.of("permission", List.of(Map.of(
			"action", "read", "constraint", Map.of("leftOperand", "vc:country", "rightOperand", "DE"))));
	private static final Map<String, Object> RESOURCE_POLICY = Map.of("permission", List.of(Map.of(
			"action", "read", "constraint", Map.of("leftOperand", "tmf:catalog", "rightOperand", "c"))));
	private static final ValidationRequest.Resource RESOURCE = new ValidationRequest.Resource("ProductOffering",
			"urn:ngsi-ld:ProductOffering:1", "urn:ngsi-ld:Catalog:1", List.of("urn:ngsi-ld:Category:1"),
			List.of(new ValidationRequest.RelatedParty("provider", "did:web:provider.example")));
	private static final Subject SUBJECT = Subject.of(IDENTITY, CLAIMS, EXPIRES_AT);

	/** The arguments of one {@link DecisionKey#of} call. */
	private record Input(String uid, Map<String, Object> policy, String scope, String action,
			ValidationRequest.Resource resource, Subject subject) {

		DecisionKey key() {
			return DecisionKey.of(uid, policy, scope, action, resource, subject);
		}

		Input withResource(ValidationRequest.Resource other) {
			return new Input(uid, policy, scope, action, other, subject);
		}
	}

	private static Input base(Map<String, Object> policy) {
		return new Input(UID, policy, SCOPE, ACTION, RESOURCE, SUBJECT);
	}

	private static Stream<Arguments> differingInputs() {
		Input base = base(SUBJECT_ONLY_POLICY);
		return Stream.of(
				Arguments.of("fingerprint", new Input(UID, SUBJECT_ONLY_POLICY, SCOPE, ACTION, RESOURCE,
						Subject.of(IDENTITY, Map.of("verifiableCredential", List.of()), EXPIRES_AT))),
				Arguments.of("scope", new Input(UID, SUBJECT_ONLY_POLICY, "contract.negotiation", ACTION, RESOURCE,
						SUBJECT)),
				Arguments.of("action", new Input(UID, SUBJECT_ONLY_POLICY, SCOPE, "order", RESOURCE, SUBJECT)),
				Arguments.of("identity", new Input(UID, SUBJECT_ONLY_POLICY, SCOPE, ACTION, RESOURCE,
						Subject.of("did:web:other.example", CLAIMS, EXPIRES_AT))),
				Arguments.of("anonymous subject", new Input(UID, SUBJECT_ONLY_POLICY, SCOPE, ACTION, RESOURCE,
						Subject.ANONYMOUS)),
				Arguments.of("policy body", new Input(UID, Map.of("permission", List.of(Map.of("action", "read",
						"constraint", Map.of("leftOperand", "vc:country", "rightOperand", "AT")))), SCOPE, ACTION,
						RESOURCE, SUBJECT)),
				Arguments.of("uid", new Input("urn:policy:2", SUBJECT_ONLY_POLICY, SCOPE, ACTION, RESOURCE, SUBJECT)),
				Arguments.of("missing scope", new Input(UID, SUBJECT_ONLY_POLICY, null, ACTION, RESOURCE, SUBJECT)),
				Arguments.of("scope moved to action", new Input(UID, SUBJECT_ONLY_POLICY, null, SCOPE, RESOURCE,
						SUBJECT)))
				.map(arguments -> Arguments.of(arguments.get()[0], base, arguments.get()[1]));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("differingInputs")
	void aDifferentInputGivesADifferentKey(String description, Input base, Input changed) {
		assertNotEquals(base.key(), changed.key());
	}

	@Test
	void aRefreshedTokenWithTheSameClaimsKeepsTheKey() {
		Input refreshed = new Input(UID, SUBJECT_ONLY_POLICY, SCOPE, ACTION, RESOURCE,
				Subject.of(IDENTITY, CLAIMS, EXPIRES_AT.plusSeconds(3600)));
		assertEquals(base(SUBJECT_ONLY_POLICY).key(), refreshed.key());
	}

	private static Stream<Arguments> resourceChanges() {
		List<UnaryOperator<ValidationRequest.Resource>> changes = List.of(
				r -> new ValidationRequest.Resource("Catalog", r.id(), r.catalog(), r.category(), r.relatedParty()),
				r -> new ValidationRequest.Resource(r.type(), "urn:other", r.catalog(), r.category(), r.relatedParty()),
				r -> new ValidationRequest.Resource(r.type(), r.id(), "urn:other", r.category(), r.relatedParty()),
				r -> new ValidationRequest.Resource(r.type(), r.id(), r.catalog(), List.of("urn:other"),
						r.relatedParty()),
				r -> new ValidationRequest.Resource(r.type(), r.id(), r.catalog(), r.category(),
						List.of(new ValidationRequest.RelatedParty("customer", "did:web:provider.example"))),
				r -> new ValidationRequest.Resource(r.type(), r.id(), r.catalog(), r.category(),
						List.of(new ValidationRequest.RelatedParty("provider", "did:web:other.example"))),
				r -> null);
		List<String> names = List.of("type", "id", "catalog", "category", "relatedParty role", "relatedParty did",
				"no resource");
		return Stream.iterate(0, i -> i < changes.size(), i -> i + 1)
				.map(i -> Arguments.of(names.get(i), changes.get(i).apply(RESOURCE)));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("resourceChanges")
	void aDifferentResourceKeepsTheKeyOfASubjectOnlyPolicy(String field, ValidationRequest.Resource other) {
		Input base = base(SUBJECT_ONLY_POLICY);
		assertEquals(base.key(), base.withResource(other).key());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("resourceChanges")
	void aDifferentResourceChangesTheKeyOfAResourceConstrainedPolicy(String field,
			ValidationRequest.Resource other) {
		Input base = base(RESOURCE_POLICY);
		assertNotEquals(base.key(), base.withResource(other).key());
	}

	private static Stream<Arguments> sparseInputs() {
		return Stream.of(
				Arguments.of("anonymous subject", new Input(UID, RESOURCE_POLICY, SCOPE, ACTION, RESOURCE,
						Subject.ANONYMOUS)),
				Arguments.of("null resource", base(RESOURCE_POLICY).withResource(null)),
				Arguments.of("null catalog, category and relatedParty", base(RESOURCE_POLICY).withResource(
						new ValidationRequest.Resource("Catalog", "urn:ngsi-ld:Catalog:1", null, null, null))),
				Arguments.of("null relatedParty element", base(RESOURCE_POLICY).withResource(
						new ValidationRequest.Resource("Catalog", "urn:ngsi-ld:Catalog:1", null,
								Arrays.asList("urn:c", null), Arrays.asList(
								new ValidationRequest.RelatedParty(null, "did:web:x"), null)))),
				Arguments.of("null policy and uid", new Input(null, null, SCOPE, ACTION, RESOURCE, SUBJECT)));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("sparseInputs")
	void acceptsAbsentInputs(String description, Input input) {
		DecisionKey key = input.key();
		assertTrue(key.value().matches(SHA_256_HEX_PATTERN), key.value());
		assertEquals(key.value(), key.toString());
	}

	private static Stream<Arguments> keyedRecords() {
		return Stream.of(
				Arguments.of(ValidationRequest.Resource.class, DecisionKey.RESOURCE_FIELDS),
				Arguments.of(ValidationRequest.RelatedParty.class, DecisionKey.RELATED_PARTY_FIELDS));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("keyedRecords")
	void everyRecordComponentEntersTheKey(Class<? extends Record> type, Set<String> keyedFields) {
		Set<String> components = Arrays.stream(type.getRecordComponents())
				.map(RecordComponent::getName)
				.collect(Collectors.toSet());
		assertEquals(components, keyedFields, "A new field must be added to the decision key on purpose.");
	}
}
