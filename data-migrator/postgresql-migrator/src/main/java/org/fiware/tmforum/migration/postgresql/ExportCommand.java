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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Copies every entity of the requested types from PostgreSQL back into an empty broker, the rollback after
 * PostgreSQL took writes. References stay inline. Every hub gets a new NGSI-LD subscription notifying the broker
 * deployment, registered before the hub is written, since a hub without one stays silent on the broker path. Its id
 * is derived from the hub's, so a re-run reuses the subscription of an interrupted one. A hub whose own subscription
 * the broker still holds fails the export, unless subscriptions are kept. The hub checks run before anything is
 * written, so a refused export leaves the broker untouched. A reused subscription must notify this run's endpoint.
 */
@Slf4j
class ExportCommand {

	private static final String HUB_TYPE = TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION;
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
		List<String> ordered = hubsLast(types);
		ordered.forEach(report::type);
		Map<URI, String> brokerSubscriptions = null;
		if (ordered.contains(HUB_TYPE)) {
			brokerSubscriptions = checkHubs(report.type(HUB_TYPE));
			if (brokerSubscriptions == null) {
				report.error("nothing was exported, since the " + HUB_TYPE + " check failed.");
				return report;
			}
		}
		for (String type : ordered) {
			TypeReport typeReport = report.type(type);
			try {
				exportType(type, typeReport, HUB_TYPE.equals(type) ? brokerSubscriptions : null);
			} catch (RuntimeException e) {
				log.warn("Was not able to export {}.", type, e);
				typeReport.error = message(e);
			}
			if (options.failFast() && !typeReport.succeeded()) {
				report.abort();
				break;
			}
		}
		return report;
	}

	private static String message(RuntimeException e) {
		return Objects.requireNonNullElse(e.getMessage(), e.getClass().getName());
	}

	// runs before any write, so a refusal the operator has to resolve leaves the broker untouched; null when it failed
	private Map<URI, String> checkHubs(TypeReport report) {
		try {
			if (!options.resume() && !brokerIsEmpty(HUB_TYPE, report)) {
				return null;
			}
			return brokerSubscriptions(domainClass(HUB_TYPE), report);
		} catch (RuntimeException e) {
			log.warn("Was not able to check the hubs.", e);
			report.error = message(e);
			return null;
		}
	}

	// a hub's new subscription notifies on creation, so restoring the entities it watches after it would fire events
	private static List<String> hubsLast(Collection<String> types) {
		List<String> ordered = types.stream()
				.filter(type -> !HUB_TYPE.equals(type))
				.distinct()
				.collect(Collectors.toCollection(ArrayList::new));
		if (types.contains(HUB_TYPE)) {
			ordered.add(HUB_TYPE);
		}
		return ordered;
	}

	// brokerSubscriptions is null for every type but the hubs, whose emptiness checkHubs already confirmed
	private void exportType(String type, TypeReport report, Map<URI, String> brokerSubscriptions) {
		if (brokerSubscriptions == null && !options.resume() && !brokerIsEmpty(type, report)) {
			return;
		}
		Class<?> domainClass = domainClass(type);
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

	private boolean brokerIsEmpty(String type, TypeReport report) {
		long stored = target.count(type);
		if (stored > 0) {
			report.error = String.format("the broker already holds %d entities of this type; export needs an empty "
					+ "broker. --resume continues an earlier export, but never overwrites an entity the broker "
					+ "holds.", stored);
			return false;
		}
		return true;
	}

	private Class<?> domainClass(String type) {
		return registry.domainClass(type)
				.orElseThrow(() -> new IllegalArgumentException("No domain class maps " + type + "."));
	}

	// the broker's subscription endpoints by id; null when the broker holds subscriptions that new ones would
	// duplicate and they are not to be kept, a hub that needs a new subscription has no notification endpoint, or one
	// an earlier export registered notifies another endpoint
	private Map<URI, String> brokerSubscriptions(Class<?> domainClass, TypeReport report) {
		List<BrokerSubscription> subscriptions = target.subscriptions(options.pageSize());
		// the endpoint may be null, which Collectors.toMap rejects
		Map<URI, String> ids = new HashMap<>();
		subscriptions.forEach(subscription -> ids.put(subscription.id(), subscription.endpoint()));
		boolean keep = options.keepsBrokerSubscriptions();
		if (!keep) {
			Set<String> endpoints = options.endpoints();
			List<URI> notifying = subscriptions.stream()
					.filter(subscription -> endpoints.contains(subscription.endpoint()))
					.map(BrokerSubscription::id)
					.toList();
			if (!notifying.isEmpty()) {
				report.error = String.format("the broker already holds %d subscriptions notifying %s, e.g. %s; new "
								+ "ones would duplicate the notifications. %s", notifying.size(), endpoints,
						notifying.stream().limit(ExportReport.MAX_EXAMPLES).toList(), KEEP_OR_DELETE);
				return null;
			}
		}
		HubScan scan = scanHubs(domainClass, ids);
		// a hub's own subscription, or the one an interrupted export registered for it, may notify another spelling of
		// the endpoint, so match them by id as well
		if (!keep && !scan.held().isEmpty()) {
			report.error = String.format("the broker still holds the subscriptions of %d hubs, e.g. %s, under another "
							+ "notification endpoint; new ones would duplicate the notifications. %s", scan.held().size(),
					scan.held().stream().limit(ExportReport.MAX_EXAMPLES).toList(), KEEP_OR_DELETE);
			return null;
		}
		if (!scan.withoutEndpoint().isEmpty()) {
			report.error = String.format("%d hubs need a new subscription but have no notification endpoint, e.g. %s",
					scan.withoutEndpoint().size(),
					scan.withoutEndpoint().stream().limit(ExportReport.MAX_EXAMPLES).toList());
			return null;
		}
		if (!scan.misdirected().isEmpty()) {
			report.error = String.format("%d hubs have a subscription from an earlier export that notifies another "
							+ "endpoint than this run's, e.g. %s; delete those subscriptions, and these hubs if the broker "
							+ "holds them, then re-run with --resume.", scan.misdirected().size(),
					scan.misdirected().stream().limit(ExportReport.MAX_EXAMPLES).toList());
			return null;
		}
		return ids;
	}

	private HubScan scanHubs(Class<?> domainClass, Map<URI, String> brokerSubscriptions) {
		Set<URI> held = new LinkedHashSet<>();
		List<String> withoutEndpoint = new ArrayList<>();
		List<String> misdirected = new ArrayList<>();
		int offset = 0;
		long sourceCount;
		do {
			ExportSource.Page page = source.page(HUB_TYPE, domainClass, offset, options.pageSize());
			sourceCount = page.totalCount();
			for (Object entity : page.entities()) {
				TMForumSubscription hub = (TMForumSubscription) entity;
				URI stored = storedSubscription(hub);
				boolean storedHeld = stored != null && brokerSubscriptions.containsKey(stored);
				if (storedHeld) {
					held.add(stored);
				}
				URI exported = exportSubscriptionId(hub.getId());
				if (brokerSubscriptions.containsKey(exported)) {
					held.add(exported);
				}
				if (!storedHeld) {
					missingEndpoint(hub).ifPresent(withoutEndpoint::add);
					if (brokerSubscriptions.containsKey(exported)) {
						misdirected(hub, exported, brokerSubscriptions).ifPresent(misdirected::add);
					}
				}
			}
			if (page.entities().isEmpty()) {
				break;
			}
			offset += page.entities().size();
		} while (offset < sourceCount);
		return new HubScan(held, withoutEndpoint, misdirected);
	}

	// e.g. registered by an interrupted run with another --notification-endpoint; empty when the hub has no endpoint,
	// which missingEndpoint reports
	private Optional<String> misdirected(TMForumSubscription hub, URI subscriptionId,
			Map<URI, String> brokerSubscriptions) {
		URI expected;
		try {
			expected = options.notificationEndpoint(hub.getEntities());
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
		String registered = brokerSubscriptions.get(subscriptionId);
		if (expected.toString().equals(registered)) {
			return Optional.empty();
		}
		return Optional.of(String.format("%s: subscription %s notifies %s, not %s", hub.getId(), subscriptionId,
				registered, expected));
	}

	// empty when the hub's endpoint resolves, or when a resumed run skips the hub since the broker already holds it
	private Optional<String> missingEndpoint(TMForumSubscription hub) {
		try {
			options.notificationEndpoint(hub.getEntities());
			return Optional.empty();
		} catch (IllegalArgumentException e) {
			if (options.resume() && target.hub(hub.getId()).isPresent()) {
				return Optional.empty();
			}
			return Optional.of(hub.getId() + ": " + e.getMessage());
		}
	}

	private static URI storedSubscription(TMForumSubscription hub) {
		return hub.getSubscription() == null ? null : hub.getSubscription().getId();
	}

	private boolean exportHub(TMForumSubscription hub, Map<URI, String> brokerSubscriptions, TypeReport report) {
		URI stored = storedSubscription(hub);
		// the broker still holds the hub's subscription, e.g. after an import from this very broker
		if (stored != null && brokerSubscriptions.containsKey(stored)) {
			if (!options.keepsBrokerSubscriptions()) {
				// stored after the check in brokerSubscriptions, e.g. by a write during the export
				report.fail(String.format("%s: the broker still holds its subscription %s; delete it first, or use "
						+ "--keep-broker-subscriptions to keep it.", hub.getId(), stored));
				return false;
			}
			report.subscriptionsKept++;
			return write(hub, report) != Outcome.FAILED;
		}
		if (options.resume()) {
			Optional<TMForumSubscription> exported = target.hub(hub.getId());
			if (exported.isPresent()) {
				return skipExported(exported.get(), brokerSubscriptions, report);
			}
		}
		Subscription subscription;
		try {
			subscription = subscription(hub);
		} catch (IllegalArgumentException e) {
			report.fail(hub.getId() + ": " + e.getMessage());
			return false;
		}
		hub.setSubscription(subscription);
		// registered by an interrupted export that did not write the hub
		boolean reused = brokerSubscriptions.containsKey(subscription.getId());
		if (reused) {
			Optional<String> misdirected = misdirected(hub, subscription.getId(), brokerSubscriptions);
			if (misdirected.isPresent()) {
				report.fail(misdirected.get() + "; delete it and re-run with --resume.");
				return false;
			}
			report.subscriptionsKept++;
		} else if (options.dryRun()) {
			report.subscriptionsRegistered++;
		} else {
			try {
				target.registerSubscription(subscription);
			} catch (RuntimeException e) {
				log.warn("Was not able to register the subscription of {}.", hub.getId(), e);
				report.fail(hub.getId() + ": registering its subscription failed: " + e.getMessage());
				return false;
			}
			report.subscriptionsRegistered++;
		}
		Outcome outcome = write(hub, report);
		if (outcome != Outcome.WRITTEN) {
			releaseSubscription(hub.getId(), subscription.getId(), reused, report);
		}
		return outcome != Outcome.FAILED;
	}

	// written by an earlier export; it only notifies while the broker holds the subscription it references
	private static boolean skipExported(TMForumSubscription exported, Map<URI, String> brokerSubscriptions,
			TypeReport report) {
		URI subscription = storedSubscription(exported);
		if (subscription != null && brokerSubscriptions.containsKey(subscription)) {
			report.skipped++;
			return true;
		}
		report.fail(String.format("%s: the broker holds the hub, but not its subscription %s, so it would never "
				+ "notify; delete the hub from the broker and re-run with --resume.", exported.getId(), subscription));
		return false;
	}

	/**
	 * The id {@code export} gives the hub's subscription; derived from the hub id, so it is the same on every run.
	 */
	static URI exportSubscriptionId(URI hubId) {
		return new Subscription(UUID.nameUUIDFromBytes(hubId.toString().getBytes(StandardCharsets.UTF_8)).toString())
				.getId();
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
		return NgsiLdSubscriptionFactory.build(exportSubscriptionId(hub.getId()).toString(),
				options.notificationEndpoint(entityTypes), hub.getCallback().toString(), hub.getQuery(), entityTypes,
				hub.getFields());
	}

	// a write that failed may still have stored the hub, which then needs the subscription
	private void releaseSubscription(URI hubId, URI subscriptionId, boolean reused, TypeReport report) {
		Optional<TMForumSubscription> stored;
		try {
			stored = target.hub(hubId);
		} catch (RuntimeException e) {
			log.warn("Was not able to check whether {} was stored.", hubId, e);
			report.fail(String.format("%s: could not check whether the hub was stored, so its subscription %s is kept; "
					+ "re-run with --resume: %s", hubId, subscriptionId, e.getMessage()));
			return;
		}
		if (stored.map(ExportCommand::storedSubscription).filter(subscriptionId::equals).isPresent()) {
			log.info("{} was stored with subscription {} despite the error, the subscription is kept.", hubId,
					subscriptionId);
			return;
		}
		try {
			target.deleteSubscription(subscriptionId);
			if (reused) {
				report.subscriptionsKept--;
			} else {
				report.subscriptionsRegistered--;
			}
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

	private record HubScan(Set<URI> held, List<String> withoutEndpoint, List<String> misdirected) {
	}

	private enum Outcome {
		WRITTEN, SKIPPED, FAILED
	}
}
