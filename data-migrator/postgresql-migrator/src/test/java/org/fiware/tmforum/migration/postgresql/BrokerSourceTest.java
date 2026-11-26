package org.fiware.tmforum.migration.postgresql;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.micronaut.context.ApplicationContext;
import org.fiware.tmforum.common.mapping.ForbiddenCharacterEscaper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import picocli.CommandLine;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class BrokerSourceTest {

	private static final String READ_TIMEOUT = "1s";
	// well above the read timeout, well below a hang
	private static final Duration TEST_TIMEOUT = Duration.ofSeconds(20);
	private static final String LOOPBACK_URL = "http://127.0.0.1:%d";

	private static final String SUBSCRIPTION_ID_PREFIX = "urn:ngsi-ld:Subscription:";
	private static final String CALLBACK_ENDPOINT = "http://tmforum:8080/callback";
	private static final String SUBSCRIPTION_JSON =
			"{\"id\": \"%s\", \"type\": \"Subscription\", \"notification\": {\"endpoint\": {\"uri\": \"%s\"}}}";
	private static final int HTTP_OK = 200;
	private static final int HTTP_SERVER_ERROR = 500;

	private ApplicationContext context;
	private HttpServer stub;

	@AfterEach
	void closeAndResetEscaper() {
		if (context != null) {
			context.close();
		}
		if (stub != null) {
			stub.stop(0);
		}
		ForbiddenCharacterEscaper.setEnabled(true);
	}

	@Test
	void listingTheTypesOfASilentBrokerTimesOut() throws IOException {
		// the kernel accepts connections into the backlog, but nothing ever answers
		try (ServerSocket silentBroker = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
			BrokerSource source = new BrokerSource(sourceContext(silentBroker.getLocalPort()), EntityTypeRegistry.scan());

			IllegalStateException failure = assertTimeoutPreemptively(TEST_TIMEOUT,
					() -> assertThrows(IllegalStateException.class, source::types));

			assertInstanceOf(HttpTimeoutException.class, failure.getCause());
		}
	}

	@ParameterizedTest(name = "{0} subscriptions in pages of {1}")
	@CsvSource({"0, 2", "1, 2", "2, 2", "5, 2", "3, 100"})
	void listsEverySubscriptionPageByPage(int total, int pageSize) throws IOException {
		List<String> all = IntStream.range(0, total).mapToObj(i -> SUBSCRIPTION_ID_PREFIX + i).toList();
		startStub(exchange -> {
			Map<String, Integer> query = queryParameters(exchange.getRequestURI());
			int offset = query.get("offset");
			List<String> page = all.subList(Math.min(offset, total), Math.min(offset + query.get("limit"), total));
			respond(exchange, HTTP_OK, page.stream()
					.map(id -> String.format(SUBSCRIPTION_JSON, id, CALLBACK_ENDPOINT))
					.collect(Collectors.joining(",", "[", "]")));
		});

		List<SubscriptionStore.BrokerSubscription> listed = brokerSource().subscriptions(pageSize);

		assertEquals(all, listed.stream().map(subscription -> subscription.id().toString()).toList());
		listed.forEach(subscription -> assertEquals(CALLBACK_ENDPOINT, subscription.endpoint()));
	}

	@Test
	void listingFailsWhenTheBrokerIgnoresTheOffset() throws IOException {
		startStub(exchange -> respond(exchange, HTTP_OK, String.format("[" + SUBSCRIPTION_JSON + "]",
				SUBSCRIPTION_ID_PREFIX + 0, CALLBACK_ENDPOINT)));

		BrokerSource source = brokerSource();

		assertThrows(IllegalStateException.class, () -> source.subscriptions(1));
	}

	@Test
	void listsASubscriptionWithoutEndpoint() throws IOException {
		startStub(exchange -> respond(exchange, HTTP_OK, "[{\"id\": \"" + SUBSCRIPTION_ID_PREFIX + "0\"}]"));

		List<SubscriptionStore.BrokerSubscription> listed = brokerSource().subscriptions(2);

		assertEquals(1, listed.size());
		assertNull(listed.getFirst().endpoint());
	}

	@ParameterizedTest(name = "status {0}")
	@CsvSource({"204, true", "404, false"})
	void deletesASubscription(int status, boolean expected) throws IOException {
		startStub(exchange -> {
			assertEquals("DELETE", exchange.getRequestMethod());
			respond(exchange, status, "");
		});

		assertEquals(expected, brokerSource().deleteSubscription(URI.create(SUBSCRIPTION_ID_PREFIX + 0)));
	}

	@Test
	void deletingFailsOnAnyOtherStatus() throws IOException {
		startStub(exchange -> respond(exchange, HTTP_SERVER_ERROR, "broken"));

		BrokerSource source = brokerSource();

		assertThrows(IllegalStateException.class, () -> source.deleteSubscription(URI.create(SUBSCRIPTION_ID_PREFIX)));
	}

	private void startStub(HttpHandler handler) throws IOException {
		stub = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		stub.createContext("/", handler);
		stub.start();
	}

	private BrokerSource brokerSource() {
		return new BrokerSource(sourceContext(stub.getAddress().getPort()), EntityTypeRegistry.scan());
	}

	private static Map<String, Integer> queryParameters(URI uri) {
		return Arrays.stream(uri.getQuery().split("&"))
				.map(parameter -> parameter.split("="))
				.collect(Collectors.toMap(pair -> pair[0], pair -> Integer.parseInt(pair[1])));
	}

	private static void respond(HttpExchange exchange, int status, String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
		if (bytes.length > 0) {
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(bytes);
			}
		}
		exchange.close();
	}

	private ApplicationContext sourceContext(int port) {
		MigrationContexts contexts = new MigrationContexts();
		new CommandLine(contexts).parseArgs("--broker-url", String.format(LOOPBACK_URL, port));
		Map<String, Object> properties = contexts.sourceProperties();
		properties.put(BrokerSource.READ_TIMEOUT_PROPERTY, READ_TIMEOUT);
		context = ApplicationContext.builder()
				.deduceEnvironment(false)
				.deduceCloudEnvironment(false)
				.environments(contexts.sourceEnvironments().toArray(String[]::new))
				.properties(properties)
				.start();
		return context;
	}
}
