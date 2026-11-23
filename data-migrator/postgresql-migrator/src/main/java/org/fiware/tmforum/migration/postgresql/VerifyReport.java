package org.fiware.tmforum.migration.postgresql;

import org.fiware.tmforum.migration.postgresql.DocumentDiff.Difference;
import org.fiware.tmforum.migration.postgresql.DocumentDiff.Kind;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Outcome of a {@code verify} run. Failures fail the run; allowed differences and target-only keys are only listed.
 */
class VerifyReport {

	// per finding, enough to locate the cause without flooding the report
	static final int MAX_EXAMPLES = 10;
	private static final String ROW_FORMAT = "%-45s %12s %12s %12s %12s %10s%n";
	private static final String NO_COUNT = "-";

	private final VerifyOptions options;
	private final Map<String, TypeReport> types = new LinkedHashMap<>();
	private final List<String> errors = new ArrayList<>();
	private final Findings escapedKeyRows = new Findings();

	VerifyReport(VerifyOptions options) {
		this.options = options;
	}

	TypeReport type(String type) {
		return types.computeIfAbsent(type, TypeReport::new);
	}

	/**
	 * Records a failure outside any type, e.g. of the count query or the escaper flag.
	 */
	void error(String message) {
		errors.add(message);
	}

	/**
	 * The rows holding a {@value RawAttributeCheck#ESCAPE_PREFIX} key, as found by the escape check.
	 */
	void escapedKeyRows(VerifyTarget.EscapedKeyRows rows) {
		escapedKeyRows.set(rows.count(), rows.examples());
	}

	boolean succeeded() {
		return failures().isEmpty();
	}

	/**
	 * Every cause of failure, prefixed with its type where it has one.
	 */
	List<String> failures() {
		List<String> failures = new ArrayList<>(errors);
		types.values().forEach(type -> type.failures().forEach(failure -> failures.add(type.type + ": " + failure)));
		if (escapedKeyRows.count > 0) {
			failures.add(String.format("escape check: %s rows hold a %s key, e.g. %s",
					escapedKeyRows.count, RawAttributeCheck.ESCAPE_PREFIX, escapedKeyRows.examples));
		}
		return failures;
	}

	void print(PrintWriter out) {
		out.printf("Verify %s (seed %d, sample size %d, full check up to %d entities):%n",
				options.direction().name().toLowerCase(Locale.ROOT), options.seed(), options.sampleSize(),
				options.fullCheckThreshold());
		out.printf(ROW_FORMAT, "type", "broker", "target", "raw-checked", "diff-checked", "full-check");
		types.values().forEach(type -> out.printf(ROW_FORMAT, type.type,
				type.brokerCount < 0 ? NO_COUNT : type.brokerCount, type.targetCount < 0 ? NO_COUNT : type.targetCount,
				type.rawChecked, type.diffChecked, type.fullCheck));
		types.values().forEach(type -> type.allowed().forEach(allowed -> out.println("ALLOWED " + type.type + ": " + allowed)));
		failures().forEach(failure -> out.println("ERROR " + failure));
		out.println(succeeded() ? "Verify succeeded." : "Verify FAILED.");
		out.flush();
	}

	ObjectNode toJson(JsonMapper mapper) {
		ObjectNode root = mapper.createObjectNode();
		root.put("succeeded", succeeded());
		root.put("seed", options.seed());
		root.put("sampleSize", options.sampleSize());
		root.put("fullCheckThreshold", options.fullCheckThreshold());
		root.put("sourceEscaped", options.sourceEscaped());
		root.put("direction", options.direction().name());
		ArrayNode failures = root.putArray("failures");
		failures().forEach(failures::add);
		root.set("escapedKeyRows", escapedKeyRows.toJson(mapper));
		ObjectNode typeNodes = root.putObject("types");
		types.values().forEach(type -> typeNodes.set(type.type, type.toJson(mapper)));
		return root;
	}

	void write(Path file) {
		JsonMapper mapper = JsonMapper.builder().build();
		mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), toJson(mapper));
	}

	/**
	 * How often something was found, with the first {@value #MAX_EXAMPLES} occurrences.
	 */
	static final class Findings {
		long count;
		final List<String> examples = new ArrayList<>();

		void add(String example) {
			count++;
			if (examples.size() < MAX_EXAMPLES) {
				examples.add(example);
			}
		}

		/**
		 * Replaces the findings with a count found elsewhere, e.g. by SQL, keeping at most {@value #MAX_EXAMPLES} examples.
		 */
		void set(long total, List<String> found) {
			count = total;
			examples.clear();
			found.stream().limit(MAX_EXAMPLES).forEach(examples::add);
		}

		ObjectNode toJson(JsonMapper mapper) {
			ObjectNode node = mapper.createObjectNode();
			node.put("count", count);
			ArrayNode exampleNodes = node.putArray("examples");
			examples.forEach(exampleNodes::add);
			return node;
		}
	}

	/**
	 * Findings of one type; {@code brokerCount} and {@code targetCount} are -1 until known.
	 */
	static final class TypeReport {
		final String type;
		long brokerCount = -1;
		long targetCount = -1;
		long rawChecked;
		long diffChecked;
		boolean fullCheck;
		final Findings missingRows = new Findings();
		final Map<String, Findings> missingAttributes = new TreeMap<>();
		final Map<String, Long> allowedMissingAttributes = new TreeMap<>();
		final Map<String, Long> targetOnlyKeys = new TreeMap<>();
		final Map<Kind, Findings> allowedDifferences = new EnumMap<>(Kind.class);
		final Findings mismatches = new Findings();
		final Findings hubQueryMismatches = new Findings();
		final Findings mappingFailures = new Findings();
		String error;

		private TypeReport(String type) {
			this.type = type;
		}

		void differences(URI id, List<Difference> differences) {
			for (Difference difference : differences) {
				String example = id + " " + difference.path() + ": " + difference.detail();
				if (difference.allowed()) {
					allowedDifferences.computeIfAbsent(difference.kind(), kind -> new Findings()).add(example);
				} else {
					mismatches.add(example);
				}
			}
		}

		void rawCheck(URI id, RawAttributeCheck.Result result) {
			rawChecked++;
			result.missing().forEach(attribute ->
					missingAttributes.computeIfAbsent(attribute, a -> new Findings()).add(id.toString()));
			result.allowedMissing().forEach(attribute -> allowedMissingAttributes.merge(attribute, 1L, Long::sum));
			result.targetOnly().forEach(key -> targetOnlyKeys.merge(key, 1L, Long::sum));
		}

		boolean succeeded() {
			return failures().isEmpty();
		}

		List<String> failures() {
			List<String> failures = new ArrayList<>();
			if (error != null) {
				failures.add(error);
			}
			if (brokerCount != targetCount) {
				failures.add(String.format("count mismatch: the broker holds %s, the target %s", count(brokerCount),
						count(targetCount)));
			}
			if (error == null && brokerCount >= 0 && rawChecked + missingRows.count < brokerCount) {
				failures.add(String.format("read %d distinct ids, but the broker counts %d; freeze writes and re-run",
						rawChecked + missingRows.count, brokerCount));
			}
			if (missingRows.count > 0) {
				failures.add(String.format("%d entities missing in the target, e.g. %s", missingRows.count,
						missingRows.examples));
			}
			missingAttributes.forEach((attribute, findings) -> failures.add(String.format(
					"attribute '%s' missing in the target on %d entities, e.g. %s", attribute, findings.count,
					findings.examples)));
			if (mismatches.count > 0) {
				failures.add(String.format("%d field differences, e.g. %s", mismatches.count, mismatches.examples));
			}
			if (hubQueryMismatches.count > 0) {
				failures.add(String.format("escape check: %d stored hub queries differ from the broker's, e.g. %s",
						hubQueryMismatches.count, hubQueryMismatches.examples));
			}
			if (mappingFailures.count > 0) {
				failures.add(String.format("%d entities could not be read for the diff, e.g. %s",
						mappingFailures.count, mappingFailures.examples));
			}
			return failures;
		}

		List<String> allowed() {
			List<String> allowed = new ArrayList<>();
			allowedMissingAttributes.forEach((attribute, count) -> allowed.add(String.format(
					"attribute '%s' dropped on %d entities", attribute, count)));
			targetOnlyKeys.forEach((key, count) -> allowed.add(String.format(
					"target-only key '%s' on %d entities", key, count)));
			allowedDifferences.forEach((kind, findings) -> allowed.add(String.format("%s on %d fields, e.g. %s",
					kind, findings.count, findings.examples)));
			return allowed;
		}

		ObjectNode toJson(JsonMapper mapper) {
			ObjectNode node = mapper.createObjectNode();
			node.put("succeeded", succeeded());
			node.put("brokerCount", brokerCount);
			node.put("targetCount", targetCount);
			node.put("rawChecked", rawChecked);
			node.put("diffChecked", diffChecked);
			node.put("fullCheck", fullCheck);
			ArrayNode failureNodes = node.putArray("failures");
			failures().forEach(failureNodes::add);
			node.set("missingRows", missingRows.toJson(mapper));
			ObjectNode missing = node.putObject("missingAttributes");
			missingAttributes.forEach((attribute, findings) -> missing.set(attribute, findings.toJson(mapper)));
			ObjectNode allowedMissing = node.putObject("allowedMissingAttributes");
			allowedMissingAttributes.forEach(allowedMissing::put);
			ObjectNode targetOnly = node.putObject("targetOnlyKeys");
			targetOnlyKeys.forEach(targetOnly::put);
			ObjectNode allowedNodes = node.putObject("allowedDifferences");
			allowedDifferences.forEach((kind, findings) -> allowedNodes.set(kind.name(), findings.toJson(mapper)));
			node.set("mismatches", mismatches.toJson(mapper));
			node.set("hubQueryMismatches", hubQueryMismatches.toJson(mapper));
			node.set("mappingFailures", mappingFailures.toJson(mapper));
			return node;
		}

		private static String count(long count) {
			return count < 0 ? "unknown" : Long.toString(count);
		}
	}
}
