package org.fiware.tmforum.accesspolicy;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import org.fiware.tmforum.accesspolicy.TestTokens.StaticJwkSetProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.fiware.tmforum.accesspolicy.TestTokens.AUDIENCE;
import static org.fiware.tmforum.accesspolicy.TestTokens.CLOCK;
import static org.fiware.tmforum.accesspolicy.TestTokens.EC_KEY;
import static org.fiware.tmforum.accesspolicy.TestTokens.EXPIRY;
import static org.fiware.tmforum.accesspolicy.TestTokens.FOREIGN_RSA_KEY;
import static org.fiware.tmforum.accesspolicy.TestTokens.NOW;
import static org.fiware.tmforum.accesspolicy.TestTokens.RSA_KEY;
import static org.fiware.tmforum.accesspolicy.TestTokens.RSA_KID;
import static org.fiware.tmforum.accesspolicy.TestTokens.SUBJECT_CLAIM_VALUE;
import static org.fiware.tmforum.accesspolicy.TestTokens.VC_ISSUER;
import static org.fiware.tmforum.accesspolicy.TestTokens.VERIFIABLE_CREDENTIAL;
import static org.fiware.tmforum.accesspolicy.TestTokens.claims;
import static org.fiware.tmforum.accesspolicy.TestTokens.properties;
import static org.fiware.tmforum.accesspolicy.TestTokens.publicSet;
import static org.fiware.tmforum.accesspolicy.TestTokens.sign;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Tests that every token defect is rejected with its own reason and a valid token yields the configured subject.
 */
class BearerTokenValidatorTest {

	private static final Duration BEYOND_SKEW = BearerTokenValidator.MAX_CLOCK_SKEW.plusSeconds(1);
	private static final String CUSTOM_IDENTITY_CLAIM = "did";
	private static final String CUSTOM_IDENTITY = "did:key:custom";
	private static final String UNKNOWN_KID = "unknown-key";

	private static Supplier<JwkSetProvider> publishing(JWK... keys) {
		return () -> new StaticJwkSetProvider(publicSet(keys));
	}

	private static Supplier<JwkSetProvider> defaultKeys() {
		return publishing(RSA_KEY, EC_KEY);
	}

	private static Arguments defect(String name, Supplier<String> token, TokenRejection expected) {
		return defect(name, token, defaultKeys(), expected);
	}

	private static Arguments defect(String name, Supplier<String> token, Supplier<JwkSetProvider> keys,
			TokenRejection expected) {
		return Arguments.of(name, token, keys, expected);
	}

	private static String signedWithRsa(JWTClaimsSet claims) {
		return sign(RSA_KEY, claims);
	}

	private static String hmacSignedWithPublicKey() {
		try {
			SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.HS256).keyID(RSA_KID).build(),
					claims().build());
			jwt.sign(new MACSigner(RSA_KEY.toRSAPublicKey().getEncoded()));
			return jwt.serialize();
		} catch (JOSEException e) {
			throw new IllegalStateException(e);
		}
	}

	private static Stream<Arguments> defects() {
		return Stream.of(
				defect("garbage", () -> "not-a-token", TokenRejection.MALFORMED),
				defect("two parts", () -> "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ4In0", TokenRejection.MALFORMED),
				defect("alg none", () -> new PlainJWT(claims().build()).serialize(),
						TokenRejection.UNSUPPORTED_ALGORITHM),
				defect("HS256 keyed with the public key", BearerTokenValidatorTest::hmacSignedWithPublicKey,
						TokenRejection.UNSUPPORTED_ALGORITHM),
				defect("foreign key under a known kid", () -> sign(FOREIGN_RSA_KEY, claims().build()),
						TokenRejection.BAD_SIGNATURE),
				defect("unknown kid", () -> sign(RSA_KEY, UNKNOWN_KID, claims().build()),
						TokenRejection.UNKNOWN_KEY),
				defect("only an encryption key under the kid", () -> signedWithRsa(claims().build()),
						() -> new StaticJwkSetProvider(new JWKSet(
								new RSAKey.Builder(RSA_KEY.toPublicJWK()).keyUse(KeyUse.ENCRYPTION).build())),
						TokenRejection.UNKNOWN_KEY),
				defect("only a key of another kty under the kid", () -> signedWithRsa(claims().build()),
						publishing(TestTokens.ecKey(RSA_KID)), TokenRejection.UNKNOWN_KEY),
				defect("expired", () -> signedWithRsa(claims().expirationTime(Date.from(NOW.minus(BEYOND_SKEW)))
						.build()), TokenRejection.EXPIRED),
				defect("missing exp", () -> signedWithRsa(claims().expirationTime(null).build()),
						TokenRejection.EXPIRED),
				defect("future nbf", () -> signedWithRsa(claims().notBeforeTime(Date.from(NOW.plus(BEYOND_SKEW)))
						.build()), TokenRejection.NOT_YET_VALID),
				defect("wrong issuer", () -> signedWithRsa(claims().issuer("https://other.example.org").build()),
						TokenRejection.WRONG_ISSUER),
				defect("wrong audience", () -> signedWithRsa(claims().audience("other").build()),
						TokenRejection.WRONG_AUDIENCE),
				defect("missing audience", () -> signedWithRsa(claims().audience((String) null).build()),
						TokenRejection.WRONG_AUDIENCE),
				defect("no identity claim", () -> signedWithRsa(claims().claim("verifiableCredential", null).build()),
						TokenRejection.NO_IDENTITY),
				defect("provider erroring", () -> signedWithRsa(claims().build()),
						() -> new StaticJwkSetProvider(Mono.error(new IOException("down")),
								Mono.error(new IOException("down"))),
						TokenRejection.KEYS_UNAVAILABLE));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("defects")
	void rejectsDefect(String name, Supplier<String> token, Supplier<JwkSetProvider> keys, TokenRejection expected) {
		BearerTokenValidator validator = new BearerTokenValidator(properties(), keys.get(), CLOCK);

		StepVerifier.create(validator.validate(token.get()))
				.expectErrorSatisfies(error -> assertEquals(expected,
						assertInstanceOf(TokenRejectedException.class, error).reason()))
				.verify();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("defects")
	void resolvesDefectToAnonymous(String name, Supplier<String> token, Supplier<JwkSetProvider> keys,
			TokenRejection expected) {
		BearerTokenValidator validator = new BearerTokenValidator(properties(), keys.get(), CLOCK);

		StepVerifier.create(validator.resolve(token.get()))
				.expectNext(Subject.ANONYMOUS)
				.verifyComplete();
	}

	@Test
	void refreshesOnceForAnUnknownKid() {
		StaticJwkSetProvider keys = new StaticJwkSetProvider(publicSet(RSA_KEY));
		BearerTokenValidator validator = new BearerTokenValidator(properties(), keys, CLOCK);

		StepVerifier.create(validator.validate(sign(RSA_KEY, UNKNOWN_KID, claims().build())))
				.expectError(TokenRejectedException.class)
				.verify();
		assertEquals(1, keys.refreshes());
	}

	@Test
	void acceptsAKeyOnlyTheRefreshedSetHolds() {
		StaticJwkSetProvider keys = new StaticJwkSetProvider(Mono.just(publicSet(EC_KEY)),
				Mono.just(publicSet(RSA_KEY, EC_KEY)));
		BearerTokenValidator validator = new BearerTokenValidator(properties(), keys, CLOCK);

		StepVerifier.create(validator.validate(signedWithRsa(claims().build())))
				.expectNextMatches(subject -> VC_ISSUER.equals(subject.identity()))
				.verifyComplete();
		assertEquals(1, keys.refreshes());
	}

	@Test
	void acceptsAnExpiryWithinTheClockSkew() {
		Instant expiry = NOW.minus(BearerTokenValidator.MAX_CLOCK_SKEW).plusSeconds(1);
		BearerTokenValidator validator = new BearerTokenValidator(properties(), defaultKeys().get(), CLOCK);

		StepVerifier.create(validator.validate(signedWithRsa(claims().expirationTime(Date.from(expiry)).build())))
				.expectNextMatches(subject -> expiry.equals(subject.expiresAt()))
				.verifyComplete();
	}

	private static Stream<Arguments> validTokens() {
		Map<String, Object> credentialOnly = Map.of("verifiableCredential", VERIFIABLE_CREDENTIAL);
		String defaultClaim = AccessPolicyProperties.DEFAULT_IDENTITY_CLAIM;
		return Stream.of(
				Arguments.of("RSA, audience set", sign(RSA_KEY, claims().build()), AUDIENCE, defaultClaim,
						VC_ISSUER, credentialOnly),
				Arguments.of("EC, audience unset", sign(EC_KEY, claims().audience((String) null).build()), null,
						defaultClaim, VC_ISSUER, credentialOnly),
				Arguments.of("RSA without kid", sign(RSA_KEY, null, claims().build()), AUDIENCE, defaultClaim,
						VC_ISSUER, credentialOnly),
				Arguments.of("EC without kid", sign(EC_KEY, null, claims().build()), AUDIENCE, defaultClaim,
						VC_ISSUER, credentialOnly),
				Arguments.of("top-level custom claim",
						sign(RSA_KEY, claims().claim(CUSTOM_IDENTITY_CLAIM, CUSTOM_IDENTITY).build()), AUDIENCE,
						CUSTOM_IDENTITY_CLAIM, CUSTOM_IDENTITY,
						Map.of("verifiableCredential", VERIFIABLE_CREDENTIAL, CUSTOM_IDENTITY_CLAIM, CUSTOM_IDENTITY)),
				Arguments.of("registered sub claim", sign(EC_KEY, claims().build()), AUDIENCE, "sub",
						SUBJECT_CLAIM_VALUE, credentialOnly));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("validTokens")
	void acceptsValidToken(String name, String token, String audience, String identityClaim, String expectedIdentity,
			Map<String, Object> expectedClaims) {
		BearerTokenValidator validator = new BearerTokenValidator(properties(audience, identityClaim),
				defaultKeys().get(), CLOCK);

		StepVerifier.create(validator.validate(token))
				.assertNext(subject -> {
					Subject expected = Subject.of(expectedIdentity, expectedClaims, EXPIRY);
					assertEquals(expected.identity(), subject.identity());
					assertEquals(expected.claims(), subject.claims());
					assertEquals(expected.expiresAt(), subject.expiresAt());
					assertEquals(expected.credentialFingerprint(), subject.credentialFingerprint());
				})
				.verifyComplete();
	}
}
