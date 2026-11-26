package org.fiware.tmforum.accesspolicy;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Keys, claims and signed tokens shared by the bearer-token tests.
 */
final class TestTokens {

	static final String ISSUER = "https://issuer.example.org";
	static final String AUDIENCE = "tmforum";
	static final String RSA_KID = "rsa-key";
	static final String EC_KID = "ec-key";
	static final String VC_ISSUER = "did:web:provider.example.org";
	static final String SUBJECT_CLAIM_VALUE = "user-42";
	static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
	static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
	static final Duration TOKEN_LIFETIME = Duration.ofHours(1);
	static final Instant EXPIRY = NOW.plus(TOKEN_LIFETIME);
	static final Map<String, Object> VERIFIABLE_CREDENTIAL = Map.of(
			"issuer", VC_ISSUER,
			"credentialSubject", Map.of("roles", List.of("REPRESENTATIVE")));

	private static final int RSA_KEY_SIZE = 2048;

	static final RSAKey RSA_KEY = rsaKey(RSA_KID, KeyUse.SIGNATURE);
	static final ECKey EC_KEY = ecKey(EC_KID);
	/** Another key under {@link #RSA_KID}, standing in for an attacker reusing a published kid. */
	static final RSAKey FOREIGN_RSA_KEY = rsaKey(RSA_KID, KeyUse.SIGNATURE);

	private TestTokens() {
	}

	static AccessPolicyProperties properties(String audience, String identityClaim) {
		AccessPolicyProperties properties = new AccessPolicyProperties();
		properties.setEnabled(true);
		properties.getJwt().setJwksUri("https://issuer.example.org/jwks");
		properties.getJwt().setIssuer(ISSUER);
		properties.getJwt().setAudience(audience);
		properties.getJwt().setIdentityClaim(identityClaim);
		return properties;
	}

	static AccessPolicyProperties properties() {
		return properties(AUDIENCE, AccessPolicyProperties.DEFAULT_IDENTITY_CLAIM);
	}

	/** Valid claims: issuer, audience, expiry, a {@code sub} and one credential. */
	static JWTClaimsSet.Builder claims() {
		return new JWTClaimsSet.Builder()
				.issuer(ISSUER)
				.audience(AUDIENCE)
				.subject(SUBJECT_CLAIM_VALUE)
				.issueTime(Date.from(NOW))
				.expirationTime(Date.from(EXPIRY))
				.claim("verifiableCredential", VERIFIABLE_CREDENTIAL);
	}

	static String sign(JWK key, JWTClaimsSet claims) {
		return sign(key, key.getKeyID(), claims);
	}

	static String sign(JWK key, String kid, JWTClaimsSet claims) {
		try {
			JWSAlgorithm algorithm = key instanceof ECKey ? JWSAlgorithm.ES256 : JWSAlgorithm.RS256;
			JWSSigner signer = key instanceof ECKey ec ? new ECDSASigner(ec) : new RSASSASigner((RSAKey) key);
			SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(algorithm).keyID(kid).build(), claims);
			jwt.sign(signer);
			return jwt.serialize();
		} catch (JOSEException e) {
			throw new IllegalStateException(e);
		}
	}

	static RSAKey rsaKey(String kid, KeyUse use) {
		try {
			return new RSAKeyGenerator(RSA_KEY_SIZE).keyID(kid).keyUse(use).generate();
		} catch (JOSEException e) {
			throw new IllegalStateException(e);
		}
	}

	static ECKey ecKey(String kid) {
		try {
			return new ECKeyGenerator(Curve.P_256).keyID(kid).keyUse(KeyUse.SIGNATURE).generate();
		} catch (JOSEException e) {
			throw new IllegalStateException(e);
		}
	}

	/** The public halves of {@code keys}, as a JWKS publishes them. */
	static JWKSet publicSet(JWK... keys) {
		return new JWKSet(Arrays.stream(keys).map(JWK::toPublicJWK).toList());
	}

	/**
	 * In-memory {@link JwkSetProvider} answering fixed results and counting refreshes.
	 */
	static final class StaticJwkSetProvider implements JwkSetProvider {

		private final Mono<JWKSet> current;
		private final Mono<JWKSet> refreshed;
		private final AtomicInteger refreshes = new AtomicInteger();

		StaticJwkSetProvider(Mono<JWKSet> current, Mono<JWKSet> refreshed) {
			this.current = current;
			this.refreshed = refreshed;
		}

		StaticJwkSetProvider(JWKSet keys) {
			this(Mono.just(keys), Mono.just(keys));
		}

		@Override
		public Mono<JWKSet> current() {
			return current;
		}

		@Override
		public Mono<JWKSet> refresh() {
			return Mono.defer(() -> {
				refreshes.incrementAndGet();
				return refreshed;
			});
		}

		int refreshes() {
			return refreshes.get();
		}
	}
}
