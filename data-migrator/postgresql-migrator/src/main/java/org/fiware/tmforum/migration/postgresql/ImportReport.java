package org.fiware.tmforum.migration.postgresql;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Per-type outcome of an import run; the run succeeded when no type has a failure or an error.
 */
class ImportReport {

	private static final String ROW_FORMAT = "%-45s %8s %8s %8s %15s %8s %15s %13s%n";
	private static final String NO_COUNT = "-";

	private final boolean dryRun;
	private final Map<String, TypeReport> types = new LinkedHashMap<>();
	private final Set<String> unmappedTypes = new TreeSet<>();
	private String typeListingFailure;
	private boolean aborted;

	ImportReport(boolean dryRun) {
		this.dryRun = dryRun;
	}

	TypeReport type(String type) {
		return types.computeIfAbsent(type, TypeReport::new);
	}

	void unmappedTypes(Set<String> unmapped) {
		unmappedTypes.addAll(unmapped);
	}

	void typeListingFailed(String message) {
		typeListingFailure = message;
	}

	void abort() {
		aborted = true;
	}

	boolean succeeded() {
		return !aborted && types.values().stream().allMatch(TypeReport::succeeded);
	}

	Set<String> unmappedTypes() {
		return Set.copyOf(unmappedTypes);
	}

	void print(PrintWriter out) {
		out.println(dryRun ? "Import (dry run, nothing written):" : "Import:");
		out.printf(ROW_FORMAT, "type", "read", "written", "skipped", "duplicate-read", "failed", "seeded-version",
				"broker-count");
		List<String> errors = new ArrayList<>();
		types.values().forEach(type -> {
			out.printf(ROW_FORMAT, type.type, type.read, type.written, type.skipped, type.duplicateRead, type.failed,
					type.seededVersion, type.brokerCount < 0 ? NO_COUNT : type.brokerCount);
			if (type.error != null) {
				errors.add(type.type + ": " + type.error);
			}
		});
		errors.forEach(error -> out.println("ERROR " + error));
		if (typeListingFailure != null) {
			out.println("Was not able to list the broker's types: " + typeListingFailure);
		} else if (!unmappedTypes.isEmpty()) {
			out.println("Broker types without a domain class, not imported: " + String.join(", ", unmappedTypes));
		}
		if (aborted) {
			out.println("Aborted at the first failure (--fail-fast).");
		}
		out.println(succeeded() ? "Import succeeded." : "Import FAILED.");
		out.flush();
	}

	/**
	 * Counters of one type. {@code written} counts would-be writes on a dry run; {@code brokerCount} is -1 until the
	 * broker reported it.
	 */
	static final class TypeReport {
		final String type;
		long read;
		long written;
		long skipped;
		long duplicateRead;
		long failed;
		long seededVersion;
		long brokerCount = -1;
		String error;

		private TypeReport(String type) {
			this.type = type;
		}

		boolean succeeded() {
			return failed == 0 && error == null;
		}
	}
}
