package org.fiware.tmforum.accesspolicy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the identity lookup, since a wrongly resolved identity would evaluate policies for the wrong caller.
 */
class IdentityClaimResolverTest {

	private static final String ISSUER = "did:web:issuer.example";
	private static final String VC_ISSUER_PATH = AccessPolicyProperties.DEFAULT_IDENTITY_CLAIM;

	private static Map<String, Object> vp(Object issuer) {
		return Map.of("verifiableCredential", List.of(Map.of("issuer", issuer)));
	}

	private static Stream<Arguments> claimPaths() {
		return Stream.of(
				Arguments.of("top-level claim", Map.of("sub", ISSUER), "sub", Optional.of(ISSUER)),
				Arguments.of("dotted path through maps", Map.of("vc", Map.of("issuer", ISSUER)), "vc.issuer",
						Optional.of(ISSUER)),
				Arguments.of("issuer as string in a one-element list", vp(ISSUER), VC_ISSUER_PATH,
						Optional.of(ISSUER)),
				Arguments.of("issuer as {id}", vp(Map.of("id", ISSUER, "name", "Issuer")), VC_ISSUER_PATH,
						Optional.of(ISSUER)),
				Arguments.of("dotted top-level name wins", Map.of(VC_ISSUER_PATH, ISSUER, "verifiableCredential",
						List.of(Map.of("issuer", "did:other"))), VC_ISSUER_PATH, Optional.of(ISSUER)),
				Arguments.of("missing segment", Map.of("verifiableCredential", List.of(Map.of("holder", ISSUER))),
						VC_ISSUER_PATH, Optional.empty()),
				Arguments.of("missing top-level claim", Map.of(), "sub", Optional.empty()),
				Arguments.of("segment below a string", Map.of("vc", ISSUER), "vc.issuer", Optional.empty()),
				Arguments.of("multi-element list", Map.of("verifiableCredential",
						List.of(Map.of("issuer", ISSUER), Map.of("issuer", ISSUER))), VC_ISSUER_PATH,
						Optional.empty()),
				Arguments.of("empty list", Map.of("verifiableCredential", List.of()), VC_ISSUER_PATH,
						Optional.empty()),
				Arguments.of("numeric terminal", Map.of("sub", 42), "sub", Optional.empty()),
				Arguments.of("list terminal", Map.of("sub", List.of(ISSUER)), "sub", Optional.empty()),
				Arguments.of("{id} with non-string id", vp(Map.of("id", 42)), VC_ISSUER_PATH, Optional.empty()),
				Arguments.of("map without id", vp(Map.of("name", "Issuer")), VC_ISSUER_PATH, Optional.empty()),
				Arguments.of("blank string", Map.of("sub", "  "), "sub", Optional.empty()),
				Arguments.of("blank id", vp(Map.of("id", "")), VC_ISSUER_PATH, Optional.empty()));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("claimPaths")
	void resolvesIdentity(String description, Map<String, Object> claims, String path, Optional<String> expected) {
		assertEquals(expected, IdentityClaimResolver.resolve(claims, path));
	}
}
