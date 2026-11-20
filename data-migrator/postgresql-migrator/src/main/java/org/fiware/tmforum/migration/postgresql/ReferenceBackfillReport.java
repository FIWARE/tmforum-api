package org.fiware.tmforum.migration.postgresql;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Per-attribute outcome of a reference backfill; it succeeded when nothing failed.
 * A failed PostgreSQL run is rolled back as a whole, so it carries no attribute rows. The broker backfill writes
 * entity by entity and records each failure as an error next to the rows.
 */
class ReferenceBackfillReport {

	private static final String ROW_FORMAT = "%-70s %18s %15s %16s%n";

	private final boolean dryRun;
	private final List<AttributeResult> attributes = new ArrayList<>();
	private final List<String> errors = new ArrayList<>();
	private String failure;

	ReferenceBackfillReport(boolean dryRun) {
		this.dryRun = dryRun;
	}

	/**
	 * One catalog entry. {@code changedEntities} counts would-be updates on a dry run and actual ones otherwise.
	 */
	record AttributeResult(String attribute, long filledReferences, long missingTargets, long changedEntities) {

		AttributeResult withChangedEntities(long updated) {
			return new AttributeResult(attribute, filledReferences, missingTargets, updated);
		}
	}

	void add(List<AttributeResult> results) {
		attributes.addAll(results);
	}

	// an exception may carry no message, and a null failure would read as success
	void fail(Exception cause) {
		failure = Objects.requireNonNullElse(cause.getMessage(), cause.getClass().getName());
	}

	/**
	 * Records a failure that did not stop the run, e.g. of a single entity.
	 */
	void error(String message) {
		errors.add(message);
	}

	boolean succeeded() {
		return failure == null && errors.isEmpty();
	}

	List<String> errors() {
		return List.copyOf(errors);
	}

	List<AttributeResult> attributes() {
		return List.copyOf(attributes);
	}

	void print(PrintWriter out) {
		out.println(dryRun ? "Reference backfill (dry run, nothing written):" : "Reference backfill:");
		out.printf(ROW_FORMAT, "attribute", "filled-references", "missing-target", "changed-entities");
		attributes.forEach(result -> out.printf(ROW_FORMAT, result.attribute(), result.filledReferences(),
				result.missingTargets(), result.changedEntities()));
		errors.forEach(error -> out.println("ERROR " + error));
		if (failure != null) {
			out.println("ERROR rolled back, nothing written: " + failure);
		}
		out.println(succeeded() ? "Reference backfill succeeded." : "Reference backfill FAILED.");
		out.flush();
	}
}
