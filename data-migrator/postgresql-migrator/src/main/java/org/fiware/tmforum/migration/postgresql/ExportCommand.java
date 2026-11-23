package org.fiware.tmforum.migration.postgresql;

import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.notification.NgsiLdSubscriptionFactory;
import org.fiware.tmforum.migration.postgresql.ExportReport.TypeReport;
import org.fiware.tmforum.migration.postgresql.SubscriptionStore.BrokerSubscription;
import org.fiware.tmforum.storage.StorageErrorReason;
import org.fiware.tmforum.storage.StorageException;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Copies every entity of the requested types from PostgreSQL back into an empty broker, the rollback after
 * PostgreSQL took writes. References stay inline. Every hub gets a new NGSI-LD subscription notifying the broker
 * deployment, registered before the hub is written, since a hub without one stays silent on the broker path. A hub
 * whose own subscription the broker still holds fails the export, unless subscriptions are kept.
 */
@Slf4j
class ExportCommand {

	private static final String KEEP_OR_DELETE = "Delete them first, or use --keep-broker-subscriptions to keep them.";

	private final ExportSource source;
	private final ExportTarget target;
	private final EntityTypeRegistry registry;
	private final ExportOptions options;

	ExportCommand(ExportSource source, ExportTarget target, EntityTypeRegistry registry, ExportOptions options) {
		this.source = source;
		this.target = target;
		this.registry = registry;
		this.options = options;
	}

	ExportReport run(Collection<String> types) {
		ExportReport report = new ExportReport(options.dryRun());
		for (String type : hubsLast(types)) {
			TypeReport typeReport = report.type(type);
			try {
				exportType(type, typeReport);
			} catch (RuntimeException e) {
				log.warn("Was not able to export {}.", type, e);
				typeReport.error = Objects.requireNonNullElse(e.getMessage(), e.getClass().getName());
			}
			if (options.failFast() && !typeReport.succeeded()) {
				report.abort();
				break;
			}
		}
		return report;
	}

	// a hub's new subscription notifies on creation, so restoring the entities it watches after it would fire events
	private static List<String> hubsLast(Collection<String> types) {
		List<String> ordered = types.stream()
				.filter(type -> !TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION.equals(type))
				.collect(Collectors.toCollection(ArrayList::new));
		if (types.contains(TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION)) {
			ordered.add(TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION);
		}
		return ordered;
	}

	private void exportType(String type, TypeReport report) {
		if (!options.resume()) {
			long stored = target.count(type);
			if (stored > 0) {
				report.error = String.format("the broker already holds %d entities of this type; export needs an empty "
						+ "broker. --resume continues an earlier export, but never overwrites an entity the broker "
						+ "holds.", stored);
				return;
			}
		}
		Class<?> domainClass = registry.domainClass(type)
				.orElseThrow(() -> new IllegalArgumentException("No domain class maps " + type + "."));
		Set<URI> brokerSubscriptions = null;
		if (TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION.equals(type)) {
			brokerSubscriptions = brokerSubscriptions(domainClass, report);
			if (brokerSubscriptions == null) {
				return;
			}
		}
		int offset = 0;
		long sourceCount;
		do {
			ExportSource.Page page = source.page(type, domainClass, offset, options.pageSize());
			sourceCount = page.totalCount();
			report.sourceCount = sourceCount;
			for (Object entity : page.entities()) {
				report.read++;
				boolean exported = brokerSubscriptions == null
						? write(entity, report) != Outcome.FAILED
						: exportHub((TMForumSubscription) entity, brokerSubscriptions, report);
				if (!exported && options.failFast()) {
					return;
				}
			}
			if (page.entities().isEmpty()) {
				break;
			}
			offset += page.entities().size();
		} while (offset < sourceCount);
		if (report.read < sourceCount) {
			report.error = String.format("read %d entities, but PostgreSQL counts %d; freeze writes and re-run with "
					+ "--resume.", report.read, sourceCount);
		}
	}

	// null when the broker holds subscriptions that new ones would duplicate and they are not to be kept
	private Set<URI> brokerSubscriptions(Class<?> domainClass, TypeReport report) {
		List<BrokerSubscription> subscriptions = target.subscriptions(options.pageSize());
		Set<URI> ids = subscriptions.stream().map(BrokerSubscription::id).collect(Collectors.toSet());
		if (options.keepsBrokerSubscriptions()) {
			return ids;
		}
		Set<String> endpoints = options.endpoints();
		List<URI> notifying = subscriptions.stream()
				.filter(subscription -> endpoints.contains(subscription.endpoint()))
				.map(BrokerSubscription::id)
				.toList();
		if (!notifying.isEmpty()) {
			report.error = String.format("the broker already holds %d subscriptions notifying %s, e.g. %s; new ones "
							+ "would duplicate the notifications. %s", notifying.size(), endpoints,
					notifying.stream().limit(ExportReport.MAX_EXAMPLES).toList(), KEEP_OR_DELETE);
			return null;
		}
		// a hub's own subscription may notify another spelling of the endpoint, so match it by id as well
		List<URI> held = heldSubscriptions(domainClass, ids);
		if (!held.isEmpty()) {
			report.error = String.format("the broker still holds the subscriptions of %d hubs, e.g. %s, under another "
							+ "notification endpoint; new ones would duplicate the notifications. %s", held.size(),
					held.stream().limit(ExportReport.MAX_EXAMPLES).toList(), KEEP_OR_DELETE);
			return null;
		}
		return ids;
	}

	private List<URI> heldSubscriptions(Class<?> domainClass, Set<URI> brokerSubscriptions) {
		List<URI> held = new ArrayList<>();
		if (brokerSubscriptions.isEmpty()) {
			return held;
		}
		int offset = 0;
		long sourceCount;
		do {
			ExportSource.Page page = source.page(TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION, domainClass, offset,
					options.pageSize());
			sourceCount = page.totalCount();
			for (Object entity : page.entities()) {
				URI stored = storedSubscription((TMForumSubscription) entity);
				if (stored != null && brokerSubscriptions.contains(stored)) {
					held.add(stored);
				}
			}
			if (page.entities().isEmpty()) {
				break;
			}
			offset += page.entities().size();
		} while (offset < sourceCount);
		return held;
	}

	private static URI storedSubscription(TMForumSubscription hub) {
		return hub.getSubscription() == null ? null : hub.getSubscription().getId();
	}

	private boolean exportHub(TMForumSubscription hub, Set<URI> brokerSubscriptions, TypeReport report) {
		URI stored = storedSubscription(hub);
		// the broker still holds the hub's subscription, e.g. after an import from this very broker
		if (stored != null && brokerSubscriptions.contains(stored)) {
			if (!options.keepsBrokerSubscriptions()) {
				// stored after the check in brokerSubscriptions, e.g. by a write during the export
				report.fail(String.format("%s: the broker still holds its subscription %s; delete it first, or use "
						+ "--keep-broker-subscriptions to keep it.", hub.getId(), stored));
				return false;
			}
			report.subscriptionsKept++;
			return write(hub, report) != Outcome.FAILED;
		}
		// written by an earlier export, together with its subscription
		if (options.resume() && target.exists(hub.getId())) {
			report.skipped++;
			return true;
		}
		Subscription subscription;
		try {
			subscription = subscription(hub);
		} catch (IllegalArgumentException e) {
			report.fail(hub.getId() + ": " + e.getMessage());
			return false;
		}
		hub.setSubscription(subscription);
		if (options.dryRun()) {
			report.subscriptionsRegistered++;
			return write(hub, report) != Outcome.FAILED;
		}
		try {
			target.registerSubscription(subscription);
		} catch (RuntimeException e) {
			log.warn("Was not able to register the subscription of {}.", hub.getId(), e);
			report.fail(hub.getId() + ": registering its subscription failed: " + e.getMessage());
			return false;
		}
		report.subscriptionsRegistered++;
		Outcome outcome = write(hub, report);
		if (outcome != Outcome.WRITTEN) {
			// a subscription without its hub would notify a hub the broker does not know
			removeSubscription(subscription.getId(), report);
		}
		return outcome != Outcome.FAILED;
	}

	/**
	 * The subscription the hub controller registers for the stored hub, notifying the configured endpoint. Throws
	 * {@link IllegalArgumentException} when the hub has no callback or no endpoint is configured for it.
	 */
	Subscription subscription(TMForumSubscription hub) {
		if (hub.getCallback() == null) {
			throw new IllegalArgumentException("the hub has no callback.");
		}
		List<String> entityTypes = hub.getEntities() == null ? List.of() : hub.getEntities();
		return NgsiLdSubscriptionFactory.build(options.notificationEndpoint(entityTypes), hub.getCallback().toString(),
				hub.getQuery(), entityTypes, hub.getFields());
	}

	private void removeSubscription(URI subscriptionId, TypeReport report) {
		try {
			target.deleteSubscription(subscriptionId);
			report.subscriptionsRegistered--;
		} catch (RuntimeException e) {
			log.warn("Was not able to remove subscription {}.", subscriptionId, e);
			report.fail(String.format("subscription %s of an unwritten hub is left on the broker: %s", subscriptionId,
					e.getMessage()));
		}
	}

	private Outcome write(Object entity, TypeReport report) {
		if (options.dryRun()) {
			report.written++;
			return Outcome.WRITTEN;
		}
		try {
			target.create(entity);
			report.written++;
			return Outcome.WRITTEN;
		} catch (StorageException e) {
			if (options.resume() && e.reason() == StorageErrorReason.CONFLICT) {
				report.skipped++;
				return Outcome.SKIPPED;
			}
			log.warn("Was not able to write {}.", id(entity), e);
			report.fail(id(entity) + ": " + e.getMessage());
		} catch (RuntimeException e) {
			log.warn("Was not able to write {}.", id(entity), e);
			report.fail(id(entity) + ": " + e.getMessage());
		}
		return Outcome.FAILED;
	}

	private static Object id(Object entity) {
		return entity instanceof EntityWithId withId ? withId.getId() : entity;
	}

	private enum Outcome {
		WRITTEN, SKIPPED, FAILED
	}
}
