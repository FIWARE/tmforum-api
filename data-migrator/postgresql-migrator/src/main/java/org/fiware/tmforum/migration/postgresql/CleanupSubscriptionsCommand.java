package org.fiware.tmforum.migration.postgresql;

import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.migration.postgresql.CleanupSubscriptionsReport.Kind;
import org.fiware.tmforum.migration.postgresql.SubscriptionStore.BrokerSubscription;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Deletes the NGSI-LD subscriptions the broker-backed service registered for its hubs and drops their
 * {@code subscription} reference from the hubs stored in PostgreSQL. Both sides run even when the other fails: each
 * is idempotent, so a re-run finishes what is left.
 */
class CleanupSubscriptionsCommand {

	private final EntitySource hubs;
	private final SubscriptionStore subscriptions;
	private final StoredHubs storedHubs;
	private final Options options;

	/**
	 * {@code callbackEndpoints} are the services' {@code <serverHost><basepath>/callback}, matched only with
	 * {@code includeOrphans}.
	 */
	record Options(int pageSize, boolean dryRun, boolean includeOrphans, Set<String> callbackEndpoints) {

		Options {
			if (pageSize < 1) {
				throw new IllegalArgumentException("--page-size must be positive.");
			}
			if (includeOrphans && callbackEndpoints.isEmpty()) {
				throw new IllegalArgumentException("--include-orphans needs at least one --callback-endpoint.");
			}
			callbackEndpoints = Set.copyOf(callbackEndpoints);
		}
	}

	CleanupSubscriptionsCommand(EntitySource hubs, SubscriptionStore subscriptions, StoredHubs storedHubs,
			Options options) {
		this.hubs = hubs;
		this.subscriptions = subscriptions;
		this.storedHubs = storedHubs;
		this.options = options;
	}

	/**
	 * Never throws: every failure is recorded in the report.
	 */
	CleanupSubscriptionsReport run() {
		CleanupSubscriptionsReport report = new CleanupSubscriptionsReport(options.dryRun(), options.includeOrphans());
		Set<URI> referenced = new LinkedHashSet<>();
		try {
			readHubs(report, referenced);
		} catch (RuntimeException e) {
			report.error("reading the hubs: " + message(e));
		}
		report.counts(Kind.REFERENCED).found.addAll(referenced);
		if (options.includeOrphans()) {
			try {
				subscriptions.subscriptions(options.pageSize()).stream()
						.filter(subscription -> options.callbackEndpoints().contains(subscription.endpoint()))
						.map(BrokerSubscription::id)
						.filter(id -> !referenced.contains(id))
						.forEach(report.counts(Kind.ORPHAN).found::add);
			} catch (RuntimeException e) {
				report.error("listing the broker's subscriptions: " + message(e));
			}
		}
		if (!options.dryRun()) {
			for (Kind kind : Kind.values()) {
				delete(report, kind);
			}
		}
		try {
			report.storedHubs(storedHubs.dropSubscriptions(options.dryRun()));
		} catch (RuntimeException e) {
			report.error("dropping the stored hubs' subscription, rolled back: " + message(e));
		}
		return report;
	}

	private void readHubs(CleanupSubscriptionsReport report, Set<URI> referenced) {
		int offset = 0;
		long brokerCount;
		do {
			EntitySource.Page page = hubs.page(TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION, offset,
					options.pageSize());
			brokerCount = page.totalCount();
			for (EntityVO entity : page.entities()) {
				report.hubs++;
				try {
					Subscription subscription = hubs.toDomain(entity, TMForumSubscription.class).getSubscription();
					if (subscription == null || subscription.getId() == null) {
						report.hubsWithoutSubscription++;
					} else {
						referenced.add(subscription.getId());
					}
				} catch (RuntimeException e) {
					report.error(String.format("mapping hub %s: %s", entity.getId(), message(e)));
				}
			}
			if (page.listed() == 0) {
				break;
			}
			offset += page.listed();
		} while (offset < brokerCount);
	}

	private void delete(CleanupSubscriptionsReport report, Kind kind) {
		CleanupSubscriptionsReport.Counts counts = report.counts(kind);
		for (URI id : counts.found) {
			try {
				if (subscriptions.deleteSubscription(id)) {
					counts.deleted++;
				} else {
					counts.alreadyMissing++;
				}
			} catch (RuntimeException e) {
				report.error(String.format("deleting subscription %s: %s", id, message(e)));
			}
		}
	}

	private static String message(Exception e) {
		return Objects.requireNonNullElse(e.getMessage(), e.getClass().getName());
	}
}
