package org.fiware.tmforum.migration.postgresql;

import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Outcome of {@code cleanup-subscriptions}, broker and PostgreSQL side; it succeeded when nothing failed.
 */
class CleanupSubscriptionsReport {

	static final String NO_STORED_SUBSCRIPTION =
			"PostgreSQL: no stored hub holds a subscription attribute, nothing to remove.";

	/**
	 * Why a subscription is deleted: a hub references it, or only its notification endpoint is this service's.
	 */
	enum Kind {
		REFERENCED, ORPHAN
	}

	/**
	 * Per kind: the subscriptions found, and of them, the deleted and the ones the broker no longer held.
	 */
	static final class Counts {
		final List<URI> found = new ArrayList<>();
		long deleted;
		long alreadyMissing;
	}

	private final boolean dryRun;
	private final boolean orphansSearched;
	private final Map<Kind, Counts> counts = new EnumMap<>(Kind.class);
	private final List<String> errors = new ArrayList<>();
	long hubs;
	long hubsWithoutSubscription;
	private StoredHubs.Outcome storedHubs;

	CleanupSubscriptionsReport(boolean dryRun, boolean orphansSearched) {
		this.dryRun = dryRun;
		this.orphansSearched = orphansSearched;
		for (Kind kind : Kind.values()) {
			counts.put(kind, new Counts());
		}
	}

	Counts counts(Kind kind) {
		return counts.get(kind);
	}

	void storedHubs(StoredHubs.Outcome result) {
		storedHubs = result;
	}

	StoredHubs.Outcome storedHubs() {
		return storedHubs;
	}

	void error(String message) {
		errors.add(message);
	}

	List<String> errors() {
		return List.copyOf(errors);
	}

	boolean succeeded() {
		return errors.isEmpty();
	}

	void print(PrintWriter out) {
		out.println(dryRun ? "Subscription cleanup (dry run, nothing deleted):" : "Subscription cleanup:");
		out.printf("hubs read: %d, without a subscription: %d%n", hubs, hubsWithoutSubscription);
		printKind(out, Kind.REFERENCED, "referenced subscriptions");
		if (orphansSearched) {
			printKind(out, Kind.ORPHAN, "orphan subscriptions");
		} else {
			out.println("orphan subscriptions: not searched, --include-orphans is not set");
		}
		if (storedHubs != null) {
			if (storedHubs.rowsWithSubscription() == 0) {
				out.println(NO_STORED_SUBSCRIPTION);
			} else {
				out.printf("PostgreSQL: %d hub rows hold a subscription attribute, %d updated%n",
						storedHubs.rowsWithSubscription(), storedHubs.rowsUpdated());
			}
		}
		errors.forEach(error -> out.println("ERROR " + error));
		out.println(succeeded() ? "Subscription cleanup succeeded." : "Subscription cleanup FAILED.");
		out.flush();
	}

	// every id on a dry run, which is the list to review; orphans always, since only their endpoint ties them here
	private void printKind(PrintWriter out, Kind kind, String label) {
		Counts kindCounts = counts.get(kind);
		out.printf("%s: %d, deleted: %d, already missing: %d%n", label, kindCounts.found.size(), kindCounts.deleted,
				kindCounts.alreadyMissing);
		if (dryRun || kind == Kind.ORPHAN) {
			kindCounts.found.forEach(id -> out.printf("  %s %s%n", dryRun ? "would delete" : "found", id));
		}
	}
}
