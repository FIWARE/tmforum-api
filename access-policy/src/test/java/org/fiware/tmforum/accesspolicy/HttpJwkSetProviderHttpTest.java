package org.fiware.tmforum.accesspolicy;

import com.sun.net.httpserver.HttpServer;
import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.fiware.tmforum.accesspolicy.TestTokens.ISSUER;
import static org.fiware.tmforum.accesspolicy.TestTokens.RSA_KEY;
import static org.fiware.tmforum.accesspolicy.TestTokens.VC_ISSUER;
import static org.fiware.tmforum.accesspolicy.TestTokens.claims;
import static org.fiware.tmforum.accesspolicy.TestTokens.publicSet;
import static org.fiware.tmforum.accesspolicy.TestTokens.sign;

/**
 * Tests the wired {@link HttpJwkSetProvider} against a local JWKS endpoint, through the Micronaut HTTP client.
 */
class HttpJwkSetProviderHttpTest {

	private static final String JWKS_PATH = "/jwks";
	private static final String MISSING_PATH = "/missing";
	private static final int OK = 200;
	private static final int EPHEMERAL_PORT = 0;
	private static final Duration VERIFY_TIMEOUT = Duration.ofSeconds(10);

	private HttpServer server;

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), EPHEMERAL_PORT), 0);
		server.createContext(JWKS_PATH, exchange -> {
			byte[] body = publicSet(RSA_KEY).toString().getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/jwk-set+json");
			exchange.sendResponseHeaders(OK, body.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(body);
			}
		});
		server.start();
	}

	@AfterEach
	void stopServer() {
		server.stop(0);
	}

	private static Stream<Arguments> endpoints() {
		return Stream.of(
				Arguments.of(JWKS_PATH, Subject.of(VC_ISSUER, Map.of("verifiableCredential",
						TestTokens.VERIFIABLE_CREDENTIAL), TestTokens.EXPIRY)),
				Arguments.of(MISSING_PATH, Subject.ANONYMOUS));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("endpoints")
	void resolvesAgainstTheFetchedKeys(String path, Subject expected) {
		String jwksUri = "http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort() + path;
		Map<String, Object> config = new HashMap<>(TestCaches.DECISIONS);
		config.putAll(Map.of(
				AccessPolicyProperties.ENABLED_PROPERTY, "true",
				AccessPolicyProperties.PREFIX + ".jwt.jwks-uri", jwksUri,
				AccessPolicyProperties.PREFIX + ".jwt.issuer", ISSUER,
				AccessPolicyProperties.PREFIX + ".jwt.audience", TestTokens.AUDIENCE,
				AccessPolicyProperties.PREFIX + ".pdp.url", AccessPolicyPropertiesTest.VALID_PDP_URL));
		try (ApplicationContext context = ApplicationContext.run(config)) {
			BearerTokenValidator validator = new BearerTokenValidator(context.getBean(AccessPolicyProperties.class),
					context.getBean(JwkSetProvider.class), TestTokens.CLOCK);

			StepVerifier.create(validator.resolve(sign(RSA_KEY, claims().build())))
					.expectNext(expected)
					.expectComplete()
					.verify(VERIFY_TIMEOUT);
		}
	}
}
