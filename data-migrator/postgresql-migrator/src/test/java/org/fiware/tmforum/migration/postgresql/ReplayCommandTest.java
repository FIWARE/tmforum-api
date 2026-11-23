package org.fiware.tmforum.migration.postgresql;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.fiware.tmforum.migration.postgresql.ReplayReport.Outcome;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Replays a request file against two stub deployments, standing in for the broker- and the PostgreSQL-backed one.
 */
class ReplayCommandTest {

	private static final String LIST_PATH = "/tmf-api/productCatalogManagement/v4/category";
	private static final String GET_PATH = LIST_PATH + "/urn:ngsi-ld:category:1";
	private static final String DIFFERING_PATH = LIST_PATH + "/urn:ngsi-ld:category:2";
	private static final String MISSING_PATH = LIST_PATH + "/urn:ngsi-ld:category:3";
	private static final String TENANT_HEADER = "X-Tenant";
	private static final Duration TIMEOUT = Duration.ofSeconds(5);
	private static final JsonMapper MAPPER = JsonMapper.builder().build();

	@TempDir
	Path directory;
	private Stub broker;
	private Stub postgres;

	@BeforeEach
	void startStubs() throws IOException {
		broker = new Stub();
		postgres = new Stub();
		broker.respond(LIST_PATH, 200, "[{\"id\":\"a\",\"href\":\"http://broker:8080" + LIST_PATH + "/a\"},{\"id\":\"b\"}]");
		postgres.respond(LIST_PATH, 200, "[{\"id\":\"b\"},{\"href\":\"http://postgres:8080" + LIST_PATH + "/a\",\"id\":\"a\"}]");
		broker.respond(GET_PATH, 200, "{\"id\":\"1\",\"name\":\"n\"}");
		postgres.respond(GET_PATH, 200, "{\"name\":\"n\",\"id\":\"1\"}");
		broker.respond(DIFFERING_PATH, 200, "{\"id\":\"2\",\"name\":\"broker\"}");
		postgres.respond(DIFFERING_PATH, 200, "{\"id\":\"2\",\"name\":\"postgres\"}");
		broker.respond(MISSING_PATH, 200, "{\"id\":\"3\"}");
		postgres.respond(MISSING_PATH, 404, "");
	}

	@AfterEach
	void stopStubs() {
		broker.server.stop(0);
		postgres.server.stop(0);
	}

	@Test
	void diffsStatusAndBodyOfEachRequest() throws IOException {
		Path requests = requests(
				"{\"method\":\"GET\",\"path\":\"" + LIST_PATH + "?limit=10\",\"headers\":{\"" + TENANT_HEADER
						+ "\":\"t1\",\"Host\":\"captured\"}}",
				"",
				"{\"method\":\"GET\",\"path\":\"" + GET_PATH + "\"}",
				"{\"method\":\"GET\",\"path\":\"" + DIFFERING_PATH + "\"}",
				"{\"method\":\"GET\",\"path\":\"" + MISSING_PATH + "\"}",
				"{\"method\":\"POST\",\"path\":\"" + LIST_PATH + "\",\"body\":{\"name\":\"new\"}}",
				"not json");

		ReplayReport report = command(false, false).run(requests);

		List<ReplayReport.Result> results = report.results();
		assertEquals(List.of(Outcome.EQUAL, Outcome.EQUAL, Outcome.DIFFERENT, Outcome.DIFFERENT, Outcome.SKIPPED_WRITE,
				Outcome.FAILED), results.stream().map(result -> result.outcome).toList());
		assertEquals(List.of(1, 3, 4, 5, 6, 7), results.stream().map(result -> result.line).toList());
		assertEquals(List.of("/name: broker \"broker\", postgresql \"postgres\""), results.get(2).differences);
		assertTrue(results.get(3).differences.getFirst().startsWith("status: broker 200, postgresql 404"),
				results.get(3).differences.toString());
		assertEquals("t1", broker.received.getFirst().tenant(), "Captured headers must be sent along.");
		assertTrue(broker.received.stream().noneMatch(request -> "POST".equals(request.method())),
				"A write must not be sent without --include-writes.");
		assertEquals(report.results().size() - 2, postgres.received.size());
		assertEquals(false, report.succeeded());
	}

	@Test
	void sendsWritesOnlyWhenIncluded() throws IOException {
		broker.respond(LIST_PATH, 201, "{\"name\":\"new\"}");
		postgres.respond(LIST_PATH, 201, "{\"name\":\"new\"}");
		Path requests = requests("{\"method\":\"post\",\"path\":\"" + LIST_PATH + "\",\"body\":{\"name\":\"new\"}}");

		ReplayReport report = command(true, false).run(requests);

		assertTrue(report.succeeded());
		assertEquals("{\"name\":\"new\"}", broker.received.getFirst().body());
		assertEquals("{\"name\":\"new\"}", postgres.received.getFirst().body());
	}

	@Test
	void aDryRunSendsNothing() throws IOException {
		Path requests = requests("{\"method\":\"GET\",\"path\":\"" + GET_PATH + "\"}");

		ReplayReport report = command(false, true).run(requests);

		assertEquals(Outcome.WOULD_SEND, report.results().getFirst().outcome);
		assertTrue(report.succeeded());
		assertTrue(broker.received.isEmpty());
	}

	@Test
	void theCommandWritesTheReportAndExitsNonZeroOnADiff() throws IOException {
		Path requests = requests("{\"method\":\"GET\",\"path\":\"" + DIFFERING_PATH + "\"}");
		Path reportFile = directory.resolve("report.json");
		StringWriter out = new StringWriter();

		int exitCode = new CommandLine(new Application()).setOut(new PrintWriter(out)).execute("replay",
				"--requests", requests.toString(), "--broker-base-url", broker.url().toString(),
				"--postgres-base-url", postgres.url().toString(), "--report", reportFile.toString());

		assertEquals(CommandLine.ExitCode.SOFTWARE, exitCode, out.toString());
		JsonNode report = MAPPER.readTree(reportFile.toFile());
		assertEquals(false, report.get("succeeded").asBoolean());
		assertEquals(1, report.path("summary").path("DIFFERENT").asInt());
		assertEquals("/name", report.path("requests").path(0).path("differences").path(0).asString()
				.split(":")[0]);
		assertTrue(out.toString().contains("Replay FAILED."), out.toString());
	}

	private ReplayCommand command(boolean includeWrites, boolean dryRun) {
		return new ReplayCommand(new HttpReplayClient(TIMEOUT), new ReplayOptions(broker.url(), postgres.url(),
				List.of("/tmf-api/*/v4/category"), List.of(), Set.of(), includeWrites, dryRun));
	}

	private Path requests(String... lines) throws IOException {
		return Files.write(directory.resolve("requests.jsonl"), List.of(lines));
	}

	private record Received(String method, String path, String tenant, String body) {
	}

	/**
	 * Answers each path with a fixed status and body, and records what it received.
	 */
	private static final class Stub {
		final HttpServer server;
		final Map<String, Map.Entry<Integer, String>> responses = new ConcurrentHashMap<>();
		final List<Received> received = new CopyOnWriteArrayList<>();

		Stub() throws IOException {
			server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
			server.createContext("/", this::handle);
			server.start();
		}

		void respond(String path, int status, String body) {
			responses.put(path, Map.entry(status, body));
		}

		URI url() {
			return URI.create("http://localhost:" + server.getAddress().getPort());
		}

		private void handle(HttpExchange exchange) throws IOException {
			received.add(new Received(exchange.getRequestMethod(), exchange.getRequestURI().toString(),
					exchange.getRequestHeaders().getFirst(TENANT_HEADER),
					new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
			Map.Entry<Integer, String> response = responses.getOrDefault(exchange.getRequestURI().getPath(),
					Map.entry(404, ""));
			byte[] body = response.getValue().getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(response.getKey(), body.length == 0 ? -1 : body.length);
			try (OutputStream stream = exchange.getResponseBody()) {
				stream.write(body);
			}
		}
	}
}
