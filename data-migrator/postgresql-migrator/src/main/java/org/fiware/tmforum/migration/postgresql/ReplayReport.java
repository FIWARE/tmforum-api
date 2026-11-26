package org.fiware.tmforum.migration.postgresql;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Outcome of a {@code replay} run, per request; the run succeeded when no request differs or failed and the input
 * was readable.
 */
class ReplayReport {

	/**
	 * What happened to one request.
	 */
	enum Outcome {
		EQUAL, DIFFERENT, FAILED, SKIPPED_WRITE, WOULD_SEND;

		boolean failure() {
			return this == DIFFERENT || this == FAILED;
		}
	}

	private static final int NO_STATUS = -1;

	private final boolean dryRun;
	private final List<Result> results = new ArrayList<>();
	private final List<String> errors = new ArrayList<>();

	ReplayReport(boolean dryRun) {
		this.dryRun = dryRun;
	}

	void add(Result result) {
		results.add(result);
	}

	/**
	 * Records a failure outside any request, e.g. an unreadable input file.
	 */
	void error(String message) {
		errors.add(message);
	}

	List<Result> results() {
		return List.copyOf(results);
	}

	boolean succeeded() {
		return errors.isEmpty() && results.stream().noneMatch(result -> result.outcome.failure());
	}

	Map<Outcome, Long> summary() {
		Map<Outcome, Long> summary = new EnumMap<>(Outcome.class);
		results.forEach(result -> summary.merge(result.outcome, 1L, Long::sum));
		return summary;
	}

	void print(PrintWriter out) {
		out.println(dryRun ? "Replay (dry run, nothing sent):" : "Replay:");
		results.stream().filter(result -> result.outcome.failure()).forEach(result -> {
			out.printf("%s line %d %s %s%s%n", result.outcome, result.line, result.method, result.path,
					result.error == null ? "" : ": " + result.error);
			result.differences.forEach(difference -> out.println("  " + difference));
		});
		errors.forEach(error -> out.println("ERROR " + error));
		out.println("Summary: " + summary());
		out.println(succeeded() ? "Replay succeeded." : "Replay FAILED.");
		out.flush();
	}

	ObjectNode toJson(JsonMapper mapper) {
		ObjectNode root = mapper.createObjectNode();
		root.put("succeeded", succeeded());
		root.put("dryRun", dryRun);
		ObjectNode summary = root.putObject("summary");
		summary().forEach((outcome, count) -> summary.put(outcome.name(), count));
		ArrayNode errorNodes = root.putArray("errors");
		errors.forEach(errorNodes::add);
		ArrayNode requests = root.putArray("requests");
		results.forEach(result -> requests.add(result.toJson(mapper)));
		return root;
	}

	void write(Path file) {
		JsonMapper mapper = JsonMapper.builder().build();
		mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), toJson(mapper));
	}

	/**
	 * One request's outcome; {@code line} is its 1-based line in the input file.
	 */
	static final class Result {
		final int line;
		final String method;
		final String path;
		Outcome outcome = Outcome.EQUAL;
		int brokerStatus = NO_STATUS;
		int postgresStatus = NO_STATUS;
		List<String> differences = List.of();
		String error;

		Result(int line, String method, String path) {
			this.line = line;
			this.method = method;
			this.path = path;
		}

		static Result unreadable(int line, String error) {
			return new Result(line, null, null).failed("unreadable request: " + error);
		}

		Result outcome(Outcome outcome) {
			this.outcome = outcome;
			return this;
		}

		Result failed(String error) {
			this.error = error;
			return outcome(Outcome.FAILED);
		}

		Result statuses(int broker, int postgres) {
			this.brokerStatus = broker;
			this.postgresStatus = postgres;
			return this;
		}

		Result differences(List<String> found) {
			this.differences = List.copyOf(found);
			return outcome(found.isEmpty() ? Outcome.EQUAL : Outcome.DIFFERENT);
		}

		ObjectNode toJson(JsonMapper mapper) {
			ObjectNode node = mapper.createObjectNode();
			node.put("line", line);
			node.put("method", method);
			node.put("path", path);
			node.put("outcome", outcome.name());
			if (brokerStatus != NO_STATUS) {
				node.put("brokerStatus", brokerStatus);
				node.put("postgresStatus", postgresStatus);
			}
			if (error != null) {
				node.put("error", error);
			}
			ArrayNode differenceNodes = node.putArray("differences");
			differences.forEach(differenceNodes::add);
			return node;
		}
	}
}
