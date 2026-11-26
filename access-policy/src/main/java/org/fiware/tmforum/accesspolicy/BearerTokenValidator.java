package org.fiware.tmforum.accesspolicy;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.AsymmetricJWK;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.proc.JWSVerifierFactory;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns a bearer token into a {@link Subject}: signature against the JWKS, then expiry, issuer, audience and identity.
 * Neither the token nor a library message about it is ever logged.
 */
@Slf4j
@Singleton
@Requires(property = AccessPolicyProperties.ENABLED_PROPERTY, value = "true")
public class BearerTokenValidator {

	/** Tolerated clock difference between the token issuer and this service. */
	static final Duration MAX_CLOCK_SKEW = Duration.ofSeconds(60);

	/** Asymmetric algorithms only: {@code none} carries no proof and HMAC would turn a public key into a secret. */
	private static final Set<JWSAlgorithm> ACCEPTED_ALGORITHMS = acceptedAlgorithms();

	private final AccessPolicyProperties.Jwt jwt;
	private final JwkSetProvider keys;
	private final Clock clock;
	private final JWSVerifierFactory verifierFactory = new DefaultJWSVerifierFactory();

	@Inject
	public BearerTokenValidator(AccessPolicyProperties properties, JwkSetProvider keys) {
		this(properties, keys, Clock.systemUTC());
	}

	BearerTokenValidator(AccessPolicyProperties properties, JwkSetProvider keys, Clock clock) {
		this.jwt = properties.getJwt();
		this.keys = keys;
		this.clock = clock;
	}

	/**
	 * Resolves the token's subject, {@link Subject#ANONYMOUS} for any token that is not valid. Never errors.
	 */
	public Mono<Subject> resolve(String token) {
		return validate(token).onErrorResume(error -> {
			if (error instanceof TokenRejectedException rejected) {
				log.debug("Bearer token rejected: {}", rejected.reason());
			} else {
				log.debug("Bearer token validation failed with {}", error.getClass().getName());
			}
			return Mono.just(Subject.ANONYMOUS);
		});
	}

	/** Emits the token's subject or errors with {@link TokenRejectedException}. */
	Mono<Subject> validate(String token) {
		return Mono.defer(() -> {
			SignedJWT signedJwt = parse(token);
			JWTClaimsSet claims = claimsOf(signedJwt);
			JWSHeader header = signedJwt.getHeader();
			if (!ACCEPTED_ALGORITHMS.contains(header.getAlgorithm())) {
				throw new TokenRejectedException(TokenRejection.UNSUPPORTED_ALGORITHM);
			}
			JWKSelector selector = new JWKSelector(JWKMatcher.forJWSHeader(header));
			return candidateKeys(selector)
					.map(candidates -> {
						verifySignature(signedJwt, candidates);
						return subjectOf(claims);
					});
		});
	}

	private Mono<List<JWK>> candidateKeys(JWKSelector selector) {
		return keys.current()
				.map(selector::select)
				.flatMap(candidates -> candidates.isEmpty() ? keys.refresh().map(selector::select) : Mono.just(candidates))
				.switchIfEmpty(Mono.error(() -> new TokenRejectedException(TokenRejection.KEYS_UNAVAILABLE)))
				.onErrorMap(error -> !(error instanceof TokenRejectedException),
						error -> new TokenRejectedException(TokenRejection.KEYS_UNAVAILABLE))
				.flatMap(candidates -> candidates.isEmpty()
						? Mono.error(new TokenRejectedException(TokenRejection.UNKNOWN_KEY))
						: Mono.just(candidates));
	}

	private void verifySignature(SignedJWT signedJwt, List<JWK> candidates) {
		for (JWK candidate : candidates) {
			if (!(candidate instanceof AsymmetricJWK asymmetric)) {
				throw new TokenRejectedException(TokenRejection.UNSUPPORTED_ALGORITHM);
			}
			JWSVerifier verifier;
			try {
				verifier = verifierFactory.createJWSVerifier(signedJwt.getHeader(), asymmetric.toPublicKey());
			} catch (JOSEException e) {
				throw new TokenRejectedException(TokenRejection.UNSUPPORTED_ALGORITHM);
			}
			try {
				if (signedJwt.verify(verifier)) {
					return;
				}
			} catch (JOSEException | IllegalStateException e) {
				// a verifier that throws has not verified, the next candidate may
			}
		}
		throw new TokenRejectedException(TokenRejection.BAD_SIGNATURE);
	}

	private Subject subjectOf(JWTClaimsSet claims) {
		Instant now = clock.instant();
		Date expiry = claims.getExpirationTime();
		if (expiry == null || !expiry.toInstant().isAfter(now.minus(MAX_CLOCK_SKEW))) {
			throw new TokenRejectedException(TokenRejection.EXPIRED);
		}
		Date notBefore = claims.getNotBeforeTime();
		if (notBefore != null && !notBefore.toInstant().isBefore(now.plus(MAX_CLOCK_SKEW))) {
			throw new TokenRejectedException(TokenRejection.NOT_YET_VALID);
		}
		if (!jwt.getIssuer().equals(claims.getIssuer())) {
			throw new TokenRejectedException(TokenRejection.WRONG_ISSUER);
		}
		if (jwt.getAudience() != null && !claims.getAudience().contains(jwt.getAudience())) {
			throw new TokenRejectedException(TokenRejection.WRONG_AUDIENCE);
		}
		String identity = IdentityClaimResolver.resolve(claims.getClaims(), jwt.getIdentityClaim())
				.orElseThrow(() -> new TokenRejectedException(TokenRejection.NO_IDENTITY));
		Map<String, Object> credentials = new LinkedHashMap<>(claims.getClaims());
		credentials.keySet().removeAll(JWTClaimsSet.getRegisteredNames());
		return Subject.of(identity, credentials, expiry.toInstant());
	}

	private static SignedJWT parse(String token) {
		try {
			return SignedJWT.parse(token);
		} catch (ParseException e) {
			throw new TokenRejectedException(parseFailure(token));
		}
	}

	/** A token that is a well-formed JWT, yet not a signed one, is unsigned or encrypted. */
	private static TokenRejection parseFailure(String token) {
		try {
			JWTParser.parse(token);
			return TokenRejection.UNSUPPORTED_ALGORITHM;
		} catch (ParseException e) {
			return TokenRejection.MALFORMED;
		}
	}

	private static JWTClaimsSet claimsOf(SignedJWT signedJwt) {
		try {
			return signedJwt.getJWTClaimsSet();
		} catch (ParseException e) {
			throw new TokenRejectedException(TokenRejection.MALFORMED);
		}
	}

	private static Set<JWSAlgorithm> acceptedAlgorithms() {
		Set<JWSAlgorithm> accepted = new HashSet<>(JWSAlgorithm.Family.RSA);
		accepted.addAll(JWSAlgorithm.Family.EC);
		return Set.copyOf(accepted);
	}
}
