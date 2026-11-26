package org.fiware.tmforum.accesspolicy;

import io.micronaut.context.ApplicationContext;
import io.micronaut.json.JsonMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Pins the PDP request's wire shape against committed fixtures, using the mapper the deployed modules configure.
 */
class ValidationRequestSerializationTest {

	/** Every deployed module sets this; Micronaut's own default, {@code NON_EMPTY}, would drop empty lists. */
	static final Map<String, Object> DEPLOYED_JACKSON_CONFIGURATION =
			Map.of("jackson.serialization-inclusion", "non_null");

	// Deliberately not ValidationRequest.PROTOCOL, so changing the constant breaks this test.
	private static final String EXPECTED_PROTOCOL = "tmf";
	private static final String FIXTURE_DIRECTORY = "/pdp/";
	private static final String IDENTITY = "did:web:fancy-marketplace.biz";
	private static final Instant EXPIRES_AT = Instant.parse("2026-11-26T12:00:00Z");
	private static final String SCOPE = "catalog";
	private static final String READ = "read";
	private static final String UPDATE = "update";
	private static final String OFFERING_TYPE = "ProductOffering";
	private static final String OFFERING_ID = "urn:ngsi-ld:ProductOffering:42";
	private static final String PUBLIC_CATALOG = "urn:ngsi-ld:Catalog:public";
	private static final String FULL_POLICY_UID = "urn:uuid:7e4c2f1a-0b8d-4c3e-9f6a-1d2b3c4d5e6f";
	private static final String READ_ALL_POLICY_UID = "urn:uuid:0f1e2d3c-4b5a-4968-8776-655443322110";
	private static final String JWT_CREDENTIAL = "eyJhbGciOiJFUzI1NiIsInR5cCI6InZjK2p3dCJ9"
			+ ".eyJpc3MiOiJkaWQ6d2ViOmZhbmN5LW1hcmtldHBsYWNlLmJpeiJ9.c2lnbmF0dXJl";

	private static ApplicationContext context;
	private static JsonMapper jsonMapper;

	@BeforeAll
	static void startContext() {
		context = ApplicationContext.run(DEPLOYED_JACKSON_CONFIGURATION);
		jsonMapper = context.getBean(JsonMapper.class);
	}

	@AfterAll
	static void stopContext() {
		context.close();
	}

	private static Map<String, Object> policy(String uid, Map<String, Object> permission) {
		Map<String, Object> policy = new LinkedHashMap<>();
		policy.put("@context", "http://www.w3.org/ns/odrl.jsonld");
		policy.put("@type", "Policy");
		policy.put("uid", uid);
		policy.put("permission", List.of(permission));
		return policy;
	}

	private static Map<String, Object> fullPolicy() {
		return policy(FULL_POLICY_UID, Map.of("action", READ, "target", OFFERING_ID));
	}

	private static Map<String, Object> readAllPolicy() {
		return policy(READ_ALL_POLICY_UID, Map.of("action", READ));
	}

	private static Map<String, Object> credential() {
		return Map.of(
				"@context", List.of("https://www.w3.org/2018/credentials/v1"),
				"type", List.of("VerifiableCredential", "MarketplaceCredential"),
				"issuer", IDENTITY,
				"credentialSubject", Map.of("id", "did:key:holder", "role", "operator"));
	}

	// Claims other than the credentials must not reach the PDP.
	private static Subject subjectPresenting(Object verifiableCredential) {
		return Subject.of(IDENTITY, Map.of(
				ValidationRequest.VERIFIABLE_CREDENTIAL_CLAIM, verifiableCredential,
				"roles", List.of("admin")), EXPIRES_AT);
	}

	private static ValidationRequest.Resource fullResource() {
		return new ValidationRequest.Resource(OFFERING_TYPE, OFFERING_ID, PUBLIC_CATALOG,
				List.of("urn:ngsi-ld:Category:compute"),
				List.of(new ValidationRequest.RelatedParty("provider", "did:web:mp-operations.org")));
	}

	private static Stream<Arguments> requests() {
		return Stream.of(
				Arguments.of("validation-request-full.json",
						ValidationRequest.of(fullPolicy(), SCOPE, READ, fullResource(),
								subjectPresenting(List.of(credential())))),
				Arguments.of("validation-request-full.json",
						ValidationRequest.of(fullPolicy(), SCOPE, READ, fullResource(),
								subjectPresenting(credential()))),
				Arguments.of("validation-request-null-credential.json",
						ValidationRequest.of(fullPolicy(), SCOPE, READ, fullResource(),
								subjectPresenting(Arrays.asList(null, credential())))),
				Arguments.of("validation-request-anonymous.json",
						ValidationRequest.of(readAllPolicy(), SCOPE, READ,
								new ValidationRequest.Resource("Category", "urn:ngsi-ld:Category:compute",
										null, null, null),
								Subject.ANONYMOUS)),
				Arguments.of("validation-request-empty-lists.json",
						ValidationRequest.of(readAllPolicy(), SCOPE, READ,
								new ValidationRequest.Resource(OFFERING_TYPE, "urn:ngsi-ld:ProductOffering:43",
										PUBLIC_CATALOG, List.of(), List.of()),
								Subject.ANONYMOUS)),
				Arguments.of("validation-request-jwt-credential.json",
						ValidationRequest.of(policy(FULL_POLICY_UID, Map.of("action", UPDATE)), SCOPE, UPDATE,
								new ValidationRequest.Resource(OFFERING_TYPE, OFFERING_ID, null, null, null),
								subjectPresenting(JWT_CREDENTIAL))));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("requests")
	void serialisesToTheFixture(String fixture, ValidationRequest request) throws IOException {
		assertEquals(readFixture(fixture), toTree(request));
	}

	@Test
	void fixesTheProtocol() throws IOException {
		ValidationRequest request = ValidationRequest.of(readAllPolicy(), SCOPE, READ,
				new ValidationRequest.Resource(OFFERING_TYPE, OFFERING_ID, null, null, null), Subject.ANONYMOUS);

		Map<?, ?> payload = (Map<?, ?>) ((Map<?, ?>) toTree(request).get("jsonInput")).get("payload");
		assertEquals(EXPECTED_PROTOCOL, payload.get("protocol"));
	}

	// Plain maps and lists compare by content, so key order on either side does not matter.
	private static Map<?, ?> toTree(ValidationRequest request) throws IOException {
		return jsonMapper.readValue(jsonMapper.writeValueAsBytes(request), Map.class);
	}

	private static Map<?, ?> readFixture(String fixture) throws IOException {
		try (InputStream in = ValidationRequestSerializationTest.class
				.getResourceAsStream(FIXTURE_DIRECTORY + fixture)) {
			assertNotNull(in, fixture);
			return jsonMapper.readValue(in.readAllBytes(), Map.class);
		}
	}
}
