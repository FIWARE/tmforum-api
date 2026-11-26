package org.fiware.tmforum.accesspolicy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the subject's fingerprint, which keys cached decisions, so two credential sets must never share one.
 */
class SubjectTest {

	private static final String IDENTITY = "did:web:issuer.example";
	private static final Instant EXPIRES_AT = Instant.parse("2026-11-26T12:00:00Z");
	private static final String SHA_256_HEX_PATTERN = "[0-9a-f]{64}";

	private static Map<String, Object> credential(String type, String issuer) {
		Map<String, Object> credential = new LinkedHashMap<>();
		credential.put("type", List.of("VerifiableCredential", type));
		credential.put("issuer", issuer);
		credential.put("credentialSubject", Map.of("id", "did:key:holder", "role", "operator"));
		return credential;
	}

	private static Map<String, Object> claims(Object... keyValues) {
		Map<String, Object> claims = new LinkedHashMap<>();
		for (int i = 0; i < keyValues.length; i += 2) {
			claims.put((String) keyValues[i], keyValues[i + 1]);
		}
		return claims;
	}

	private static Map<String, Object> nullable(String key, Object value) {
		Map<String, Object> map = new HashMap<>();
		map.put(key, value);
		return map;
	}

	private static String fingerprint(Map<String, Object> claims) {
		return Subject.of(IDENTITY, claims, EXPIRES_AT).credentialFingerprint();
	}

	private static Stream<Arguments> equalCredentials() {
		Map<String, Object> nested = claims("outer", claims("b", 2, "a", 1), "z", "last");
		Map<String, Object> nestedReordered = claims("z", "last", "outer", claims("a", 1, "b", 2));
		return Stream.of(
				Arguments.of("top-level keys reordered", claims("a", 1, "b", "two"), claims("b", "two", "a", 1)),
				Arguments.of("nested keys reordered", nested, nestedReordered),
				Arguments.of("keys inside a list reordered",
						claims("verifiableCredential", List.of(claims("issuer", "x", "type", "T"))),
						claims("verifiableCredential", List.of(claims("type", "T", "issuer", "x")))),
				Arguments.of("null value", nullable("nonce", null), nullable("nonce", null)));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("equalCredentials")
	void equalCredentialsShareTheFingerprint(String description, Map<String, Object> first,
			Map<String, Object> second) {
		assertEquals(fingerprint(first), fingerprint(second));
	}

	private static Stream<Arguments> differentCredentials() {
		Map<String, Object> base = claims("verifiableCredential", List.of(credential("OperatorCredential", "did:a")));
		return Stream.of(
				Arguments.of("changed nested value", base,
						claims("verifiableCredential", List.of(credential("OperatorCredential", "did:b")))),
				Arguments.of("added credential", base,
						claims("verifiableCredential", List.of(credential("OperatorCredential", "did:a"),
								credential("MemberCredential", "did:a")))),
				Arguments.of("different credential list", base,
						claims("verifiableCredential", List.of(credential("MemberCredential", "did:a")))),
				Arguments.of("reordered credential list",
						claims("verifiableCredential", List.of(credential("A", "did:a"), credential("B", "did:a"))),
						claims("verifiableCredential", List.of(credential("B", "did:a"), credential("A", "did:a")))),
				Arguments.of("null value versus absent claim", nullable("nonce", null), Map.of()),
				Arguments.of("nested null versus absent claim", claims("vc", nullable("nonce", null)),
						claims("vc", Map.of())));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("differentCredentials")
	void differentCredentialsChangeTheFingerprint(String description, Map<String, Object> first,
			Map<String, Object> second) {
		assertNotEquals(fingerprint(first), fingerprint(second));
	}

	private static Stream<Arguments> fingerprintedClaims() {
		return Stream.of(
				Arguments.of("no claims", Map.of()),
				Arguments.of("top-level null", nullable("nonce", null)),
				Arguments.of("nested null", claims("vc", nullable("issuanceDate", null))),
				Arguments.of("null list element", claims("list", Arrays.asList("a", null))));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("fingerprintedClaims")
	void fingerprintIsLowercaseSha256Hex(String description, Map<String, Object> claims) {
		Subject subject = Subject.of(IDENTITY, claims, EXPIRES_AT);

		assertTrue(subject.credentialFingerprint().matches(SHA_256_HEX_PATTERN));
		assertEquals(claims, subject.claims());
		assertFalse(subject.isAnonymous());
	}

	@Test
	void anonymousCarriesNothing() {
		assertTrue(Subject.ANONYMOUS.isAnonymous());
		assertNull(Subject.ANONYMOUS.identity());
		assertTrue(Subject.ANONYMOUS.claims().isEmpty());
		assertNull(Subject.ANONYMOUS.expiresAt());
		assertNull(Subject.ANONYMOUS.credentialFingerprint());
	}

	private static Stream<Arguments> invalidShapes() {
		Map<String, Object> credentials = claims("verifiableCredential", List.of(credential("T", "did:a")));
		String fingerprint = fingerprint(credentials);
		return Stream.of(
				Arguments.of("no identity but claims", null, credentials, null, null),
				Arguments.of("no identity but expiry", null, Map.of(), EXPIRES_AT, null),
				Arguments.of("no identity but fingerprint", null, Map.of(), null, fingerprint),
				Arguments.of("identity without expiry", IDENTITY, credentials, null, fingerprint),
				Arguments.of("identity without fingerprint", IDENTITY, credentials, EXPIRES_AT, null),
				Arguments.of("fingerprint of other claims", IDENTITY, credentials, EXPIRES_AT,
						fingerprint(claims("other", 1))),
				Arguments.of("arbitrary fingerprint", IDENTITY, credentials, EXPIRES_AT, "arbitrary"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidShapes")
	void constructorRejectsInvalidShapes(String description, String identity, Map<String, Object> claims,
			Instant expiresAt, String credentialFingerprint) {
		assertThrows(IllegalArgumentException.class,
				() -> new Subject(identity, claims, expiresAt, credentialFingerprint));
	}

	@Test
	void constructorAcceptsTheShapeOfFactory() {
		Subject subject = Subject.of(IDENTITY, claims("a", 1), EXPIRES_AT);
		assertEquals(subject, new Subject(IDENTITY, claims("a", 1), EXPIRES_AT, subject.credentialFingerprint()));
		assertFalse(subject.isAnonymous());
	}

	@Test
	void claimsAreDeeplyImmutableCopies() {
		List<Object> credentials = new ArrayList<>(List.of(new HashMap<>(Map.of("issuer", "did:a"))));
		Map<String, Object> claims = new HashMap<>(Map.of("verifiableCredential", credentials));
		Subject subject = Subject.of(IDENTITY, claims, EXPIRES_AT);
		String fingerprint = subject.credentialFingerprint();

		credentials.add("injected");
		claims.put("added", true);

		@SuppressWarnings("unchecked")
		List<Object> copiedList = (List<Object>) subject.claims().get("verifiableCredential");
		@SuppressWarnings("unchecked")
		Map<String, Object> copiedCredential = (Map<String, Object>) copiedList.getFirst();
		assertEquals(1, copiedList.size());
		assertFalse(subject.claims().containsKey("added"));
		assertEquals(fingerprint, Subject.of(IDENTITY, subject.claims(), EXPIRES_AT).credentialFingerprint());
		assertThrows(UnsupportedOperationException.class, () -> subject.claims().put("added", true));
		assertThrows(UnsupportedOperationException.class, () -> copiedList.add("injected"));
		assertThrows(UnsupportedOperationException.class, () -> copiedCredential.put("issuer", "did:b"));
	}
}
