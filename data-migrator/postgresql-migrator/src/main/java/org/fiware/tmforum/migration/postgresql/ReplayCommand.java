package org.fiware.tmforum.migration.postgresql;

import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.migration.postgresql.ReplayReport.Outcome;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Replays captured requests against the broker-backed and the PostgreSQL-backed deployment and diffs status and body
 * of each pair of responses. Writes are skipped unless {@code --include-writes}, since they change both deployments.
 */
@Slf4j
class ReplayCommand {

	private final ReplayClient client;
	private final ReplayOptions options;
	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	ReplayCommand(ReplayClient client, ReplayOptions options) {
		this.client = client;
		this.options = options;
	}

	ReplayReport run(Path requests) {
		ReplayReport report = new ReplayReport(options.dryRun());
		List<String> lines;
		try {
			lines = Files.readAllLines(requests);
		} catch (IOException e) {
			report.error("Was not able to read " + requests + ": " + e.getMessage());
			return report;
		}
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if (!line.isBlank()) {
				report.add(replay(i + 1, line));
			}
		}
		return report;
	}

	private ReplayReport.Result replay(int lineNumber, String line) {
		ReplayRequest request;
		try {
			request = ReplayRequest.parse(line, jsonMapper);
		} catch (RuntimeException e) {
			return ReplayReport.Result.unreadable(lineNumber, e.getMessage());
		}
		ReplayReport.Result result = new ReplayReport.Result(lineNumber, request.method(), request.path());
		if (request.isWrite() && !options.includeWrites()) {
			return result.outcome(Outcome.SKIPPED_WRITE);
		}
		if (options.dryRun()) {
			return result.outcome(Outcome.WOULD_SEND);
		}
		ReplayClient.Response broker;
		ReplayClient.Response postgres;
		try {
			broker = client.send(options.brokerBaseUrl(), request);
			postgres = client.send(options.postgresBaseUrl(), request);
		} catch (RuntimeException e) {
			log.warn("Was not able to replay line {}.", lineNumber, e);
			return result.failed(Objects.requireNonNullElse(e.getMessage(), e.getClass().getName()));
		}
		result.statuses(broker.status(), postgres.status());
		List<String> differences = new ArrayList<>();
		if (broker.status() != postgres.status()) {
			differences.add(String.format("status: broker %d, postgresql %d", broker.status(), postgres.status()));
		}
		differences.addAll(diffBodies(broker.body(), postgres.body(), options.rules(request.pathOnly())));
		return result.differences(differences);
	}

	private List<String> diffBodies(String broker, String postgres, ResponseDiff.Rules rules) {
		JsonNode brokerJson = json(broker);
		JsonNode postgresJson = json(postgres);
		if (brokerJson != null && postgresJson != null) {
			return ResponseDiff.diff(brokerJson, postgresJson, rules);
		}
		if (broker.strip().equals(postgres.strip())) {
			return List.of();
		}
		return List.of("body: the bodies differ and are not both JSON");
	}

	// null for an empty or non-JSON body
	private JsonNode json(String body) {
		if (body.isBlank()) {
			return null;
		}
		try {
			return jsonMapper.readTree(body);
		} catch (JacksonException e) {
			return null;
		}
	}
}
