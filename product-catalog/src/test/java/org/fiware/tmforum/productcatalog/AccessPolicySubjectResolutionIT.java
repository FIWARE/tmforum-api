package org.fiware.tmforum.productcatalog;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.fiware.tmforum.accesspolicy.AccessPolicyProperties;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.product.ProductOffering;
import org.fiware.tmforum.productcatalog.SubjectEchoController.SubjectEcho;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * With access policies enabled, every request gets a subject from its optional bearer token, and no token, valid,
 * invalid or missing, fails the request.
 * The {@code bypass-unauthenticated} and {@code mode} dimensions land with AP-09.
 */
@MicronautTest(packages = {"org.fiware.tmforum.productcatalog"}, contextBuilder = FirstMatchConfigContextBuilder.class)
@Property(name = AccessPolicyProperties.ENABLED_PROPERTY, value = "true")
@Property(name = SubjectEchoController.ENABLED_PROPERTY, value = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AccessPolicySubjectResolutionIT extends AbstractApiIT implements TestPropertyProvider {

	private static final String JWKS_PATH = "/jwks";
	private static final String JWK_SET_MEDIA_TYPE = "application/jwk-set+json";
	private static final String ISSUER = "https://issuer.it.example.org";
	private static final String AUDIENCE = "tmforum-it";
	private static final String KID = "it-key";
	private static final int RSA_KEY_SIZE = 2048;
	private static final int EPHEMERAL_PORT = 0;
	private static final int SERVER_BACKLOG = 0;
	private static final int STOP_DELAY_SECONDS = 0;
	private static final Duration TOKEN_LIFETIME = Duration.ofHours(1);
	private static final String PRODUCT_OFFERING_PATH = "/productOffering";

	private static final RSAKey PUBLISHED_KEY = rsaKey();
	/** Another key under the published kid, so its tokens fail on the signature, never on an unknown kid. */
	private static final RSAKey FOREIGN_KEY = rsaKey();

	/**
	 * The presented tokens. {@link #A} comes first, so it meets the cold JWKS cache.
	 */
	enum TokenCase {
		A(token(PUBLISHED_KEY, "did:web:a.example.org", TOKEN_LIFETIME), "did:web:a.example.org"),
		B(token(PUBLISHED_KEY, "did:web:b.example.org", TOKEN_LIFETIME), "did:web:b.example.org"),
		NONE(null, null),
		EXPIRED(token(PUBLISHED_KEY, "did:web:a.example.org", TOKEN_LIFETIME.negated()), null),
		WRONG_SIGNATURE(token(FOREIGN_KEY, "did:web:a.example.org", TOKEN_LIFETIME), null);

		private final String token;
		/** The resolved identity; {@code null} for an anonymous subject. */
		private final String identity;

		TokenCase(String token, String identity) {
			this.token = token;
			this.identity = identity;
		}

		<T> MutableHttpRequest<T> authorize(MutableHttpRequest<T> request) {
			return token == null ? request : request.bearerAuth(token);
		}
	}

	private final AtomicInteger jwksFetches = new AtomicInteger();
	private HttpServer jwksServer;

	// field injection: TestPropertyProvider needs the instance before the context, so the constructor cannot inject
	@Inject
	@Client("/")
	private HttpClient httpClient;

	public AccessPolicySubjectResolutionIT() {
		super(null, null, null);
	}

	@Override
	protected String getEntityType() {
		return ProductOffering.TYPE_PRODUCT_OFFERING;
	}

	@Override
	public Map<String, String> getProperties() {
		jwksServer = startJwksServer();
		String jwksUri = "http://" + jwksServer.getAddress().getHostString() + ":" + jwksServer.getAddress().getPort()
				+ JWKS_PATH;
		return Map.of(
				AccessPolicyProperties.PREFIX + ".jwt.jwks-uri", jwksUri,
				AccessPolicyProperties.PREFIX + ".jwt.issuer", ISSUER,
				AccessPolicyProperties.PREFIX + ".jwt.audience", AUDIENCE);
	}

	@AfterAll
	public void stopJwksServer() {
		jwksServer.stop(STOP_DELAY_SECONDS);
	}

	@Order(1)
	@ParameterizedTest
	@EnumSource(TokenCase.class)
	public void resolvesTheSubjectOfTheToken(TokenCase tokenCase) {
		SubjectEcho echo = httpClient.toBlocking()
				.retrieve(tokenCase.authorize(HttpRequest.GET(SubjectEchoController.PATH)), SubjectEcho.class);

		assertTrue(echo.resolved(), "Every request must get a subject.");
		assertTrue(echo.propagated(),
				"The subject must survive a scheduler thread switch through the propagated request context.");
		assertEquals(tokenCase.identity == null, echo.anonymous(), "Only a valid token may resolve to an identity.");
		assertEquals(tokenCase.identity, echo.identity(), "The identity must be the token's credential issuer.");
	}

	@Order(2)
	@Test
	public void distinctCredentialsGetDistinctFingerprints() {
		String fingerprintA = fingerprintOf(TokenCase.A);
		String fingerprintB = fingerprintOf(TokenCase.B);

		assertNotNull(fingerprintA, "An identified subject must carry a fingerprint.");
		assertNotNull(fingerprintB, "An identified subject must carry a fingerprint.");
		assertNotEquals(fingerprintA, fingerprintB, "Different credentials must not share a fingerprint.");
	}

	@Order(3)
	@ParameterizedTest
	@EnumSource(TokenCase.class)
	public void listIsServedWhateverTheToken(TokenCase tokenCase) throws Exception {
		HttpResponse<String> response = callAndCatch(() -> httpClient.toBlocking()
				.exchange(tokenCase.authorize(HttpRequest.GET(PRODUCT_OFFERING_PATH)), String.class));

		assertEquals(HttpStatus.OK, response.getStatus(), "A token, valid or not, must never fail the request.");
	}

	@Order(4)
	@Test
	public void keySetIsFetchedOnce() {
		assertEquals(1, jwksFetches.get(), "The key set must be fetched once and served from the cache afterwards.");
	}

	private String fingerprintOf(TokenCase tokenCase) {
		return httpClient.toBlocking()
				.retrieve(tokenCase.authorize(HttpRequest.GET(SubjectEchoController.PATH)), SubjectEcho.class)
				.fingerprint();
	}

	private HttpServer startJwksServer() {
		try {
			HttpServer server = HttpServer.create(
					new InetSocketAddress(InetAddress.getLoopbackAddress(), EPHEMERAL_PORT), SERVER_BACKLOG);
			byte[] body = new JWKSet(PUBLISHED_KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
			server.createContext(JWKS_PATH, exchange -> serve(exchange, body));
			server.start();
			return server;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void serve(HttpExchange exchange, byte[] body) throws IOException {
		jwksFetches.incrementAndGet();
		exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, JWK_SET_MEDIA_TYPE);
		exchange.sendResponseHeaders(HttpStatus.OK.getCode(), body.length);
		try (OutputStream out = exchange.getResponseBody()) {
			out.write(body);
		}
	}

	private static String token(RSAKey key, String credentialIssuer, Duration expiresIn) {
		Instant expiry = Instant.now().plus(expiresIn);
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.issuer(ISSUER)
				.audience(AUDIENCE)
				.issueTime(Date.from(expiry.minus(TOKEN_LIFETIME)))
				.expirationTime(Date.from(expiry))
				.claim("verifiableCredential", Map.of(
						"issuer", credentialIssuer,
						"credentialSubject", Map.of("roles", List.of(credentialIssuer))))
				.build();
		try {
			SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KID).build(), claims);
			jwt.sign(new RSASSASigner(key));
			return jwt.serialize();
		} catch (JOSEException e) {
			throw new IllegalStateException(e);
		}
	}

	private static RSAKey rsaKey() {
		try {
			return new RSAKeyGenerator(RSA_KEY_SIZE).keyID(KID).keyUse(KeyUse.SIGNATURE).generate();
		} catch (JOSEException e) {
			throw new IllegalStateException(e);
		}
	}
}
