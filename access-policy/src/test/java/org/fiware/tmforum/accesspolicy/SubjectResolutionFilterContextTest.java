package org.fiware.tmforum.accesspolicy;

import com.sun.net.httpserver.HttpServer;
import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that the subject resolution beans exist exactly when the feature is enabled, and that none fetches the JWKS
 * at startup.
 */
class SubjectResolutionFilterContextTest {

	private static final String JWKS_PATH = "/jwks";
	private static final int NOT_FOUND = 404;
	private static final int NO_BODY = -1;
	private static final int EPHEMERAL_PORT = 0;
	private static final List<Class<?>> SUBJECT_RESOLUTION_BEANS = List.of(
			SubjectResolutionFilter.class, BearerTokenValidator.class, HttpJwkSetProvider.class);

	private final AtomicInteger jwksFetches = new AtomicInteger();
	private HttpServer server;

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), EPHEMERAL_PORT), 0);
		server.createContext(JWKS_PATH, exchange -> {
			jwksFetches.incrementAndGet();
			exchange.sendResponseHeaders(NOT_FOUND, NO_BODY);
			exchange.close();
		});
		server.start();
	}

	@AfterEach
	void stopServer() {
		server.stop(0);
	}

	private static Stream<Arguments> configurations() {
		return Stream.of(
				Arguments.of("enabled unset", Map.of(), false),
				Arguments.of("enabled false", Map.of(AccessPolicyProperties.ENABLED_PROPERTY, "false"), false),
				Arguments.of("enabled true", Map.of(AccessPolicyProperties.ENABLED_PROPERTY, "true",
						AccessPolicyProperties.PREFIX + ".jwt.issuer", TestTokens.ISSUER), true));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("configurations")
	void beansExistOnlyWhenEnabled(String name, Map<String, Object> configuration, boolean enabled) {
		Map<String, Object> properties = new HashMap<>(configuration);
		if (enabled) {
			properties.put(AccessPolicyProperties.PREFIX + ".jwt.jwks-uri", "http://"
					+ server.getAddress().getHostString() + ":" + server.getAddress().getPort() + JWKS_PATH);
		}
		try (ApplicationContext context = ApplicationContext.run(properties)) {
			for (Class<?> bean : SUBJECT_RESOLUTION_BEANS) {
				assertEquals(enabled, context.containsBean(bean),
						String.format("%s must exist exactly when the feature is enabled.", bean.getSimpleName()));
			}
			if (enabled) {
				context.getBean(SubjectResolutionFilter.class);
			}
			assertEquals(0, jwksFetches.get(), "The JWKS must be fetched on the first token, not at startup.");
		}
	}
}
