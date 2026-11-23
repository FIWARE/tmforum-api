package org.fiware.tmforum.migration.postgresql;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-type outcome of an export run; the run succeeded when no type has a failure or an error and no run-wide error
 * (e.g. escaper drift) was recorded.
 */
class ExportReport {

	// per type, enough to locate the cause without flooding the report
	static final int MAX_EXAMPLES = 10;
	private static final String ROW_FORMAT = "%-45s %8s %8s %8s %8s %13s %13s %12s%n";
	private static final String NO_COUNT = "-";

	private final boolean dryRun;
	private final Map<String, TypeReport> types = new LinkedHashMap<>();
	private final List<String> errors = new ArrayList<>();
	private boolean aborted;

	ExportReport(boolean dryRun) {
		this.dryRun = dryRun;
	}

	TypeReport type(String type) {
		return types.computeIfAbsent(type, TypeReport::new);
	}

	/**
	 * Records a failure outside any type.
	 */
	void error(String message) {
		errors.add(message);
	}

	void abort() {
		aborted = true;
	}

	boolean succeeded() {
		return !aborted && errors.isEmpty() && types.values().stream().allMatch(TypeReport::succeeded);
	}

	void print(PrintWriter out) {
		out.println(dryRun ? "Export (dry run, nothing written):" : "Export:");
		out.printf(ROW_FORMAT, "type", "read", "written", "skipped", "failed", "subs-created", "subs-kept",
				"source-count");
		List<String> failures = new ArrayList<>();
		types.values().forEach(type -> {
			out.printf(ROW_FORMAT, type.type, type.read, type.written, type.skipped, type.failed,
					type.subscriptionsRegistered, type.subscriptionsKept,
					type.sourceCount < 0 ? NO_COUNT : type.sourceCount);
			if (type.error != null) {
				failures.add(type.type + ": " + type.error);
			}
			type.failures.forEach(failure -> failures.add(type.type + ": " + failure));
		});
		failures.addAll(errors);
		failures.forEach(failure -> out.println("ERROR " + failure));
		if (aborted) {
			out.println("Aborted at the first failure (--fail-fast).");
		}
		out.println(succeeded() ? "Export succeeded." : "Export FAILED.");
		out.flush();
	}

	/**
	 * Counters of one type. {@code written} and {@code subscriptionsRegistered} count would-be writes on a dry run;
	 * {@code sourceCount} is -1 until PostgreSQL reported it.
	 */
	static final class TypeReport {
		final String type;
		long read;
		long written;
		long skipped;
		long failed;
		long subscriptionsRegistered;
		long subscriptionsKept;
		long sourceCount = -1;
		final List<String> failures = new ArrayList<>();
		String error;

		private TypeReport(String type) {
			this.type = type;
		}

		void fail(String failure) {
			failed++;
			if (failures.size() < MAX_EXAMPLES) {
				failures.add(failure);
			}
		}

		boolean succeeded() {
			return failed == 0 && error == null;
		}
	}
}
