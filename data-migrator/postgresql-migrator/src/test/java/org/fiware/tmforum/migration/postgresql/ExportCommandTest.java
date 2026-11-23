package org.fiware.tmforum.migration.postgresql;

import org.fiware.migrationfixture.ImportEntities;
import org.fiware.migrationfixture.ImportEntities.Plain;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.migration.postgresql.ExportReport.TypeReport;
import org.fiware.tmforum.storage.StorageErrorReason;
import org.fiware.tmforum.storage.StorageException;
import org.fiware.tmforum.storage.StorageOperation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportCommandTest {

	private static final String PLAIN = ImportEntities.PLAIN_TYPE;
	private static final String HUB = TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION;
	private static final String PRODUCT = "product";
	private static final String ORDER = "product-order";
	private static final URI ENDPOINT = URI.create("http://tmforum.example.org/callback");
	private static final URI ORDER_ENDPOINT = URI.create("http://ordering.example.org/callback");
	// the same deployment as ENDPOINT, spelled differently, as the old broker's hubs may hold it
	private static final String OTHER_ENDPOINT_SPELLING = "http://tmforum.example.org:80/callback/";
	private static final URI CALLBACK = URI.create("http://listener.example.org/hub");
	private static final String QUERY = "state==\"active\"";
	private static final int PAGE_SIZE = 2;

	private final EntityTypeRegistry registry =
			new EntityTypeRegistry(List.of(Plain.class, TMForumSubscription.class), Map.of());
	private final FakeSource source = new FakeSource();
	private final FakeTarget target = new FakeTarget();

	@Test
	void copiesEveryPageIntoAnEmptyBroker() {
		source.addPlain(3);

		ExportReport report = run(options(false, false), PLAIN);

		assertTrue(report.succeeded());
		TypeReport type = report.type(PLAIN);
		assertEquals(3, type.read);
		assertEquals(3, type.written);
		assertEquals(3, type.sourceCount);
		assertEquals(source.entities.get(PLAIN), target.created);
	}

	static Stream<Arguments> filledBrokers() {
		return Stream.of(
				// a filled broker without --resume: nothing is read or written
				Arguments.of(false, false, 1, false, false, 0, 0, 0),
				// keeping broker subscriptions still needs an empty broker
				Arguments.of(false, true, 1, false, false, 0, 0, 0),
				// a fresh broker, an id stored meanwhile: a failure
				Arguments.of(false, false, 0, true, false, 1, 0, 1),
				// a resumed run: the stored id is skipped
				Arguments.of(true, false, 1, true, true, 1, 1, 0));
	}

	@ParameterizedTest
	@MethodSource("filledBrokers")
	void needsAnEmptyBrokerUnlessResumed(boolean resume, boolean keepSubscriptions, long stored,
			boolean firstConflicts, boolean succeeds, long written, long skipped, long failed) {
		source.addPlain(2);
		target.counts.put(PLAIN, stored);
		if (firstConflicts) {
			target.store(source.entities.get(PLAIN).getFirst());
		}

		TypeReport report = run(options(resume, keepSubscriptions, false), PLAIN).type(PLAIN);

		assertEquals(succeeds, report.succeeded(), String.valueOf(report.error));
		assertEquals(written, report.written);
		assertEquals(skipped, report.skipped);
		assertEquals(failed, report.failed);
	}

	@Test
	void registersANewSubscriptionForEveryHubReplacingAStaleOne() {
		TMForumSubscription imported = source.addHub(List.of(PRODUCT), URI.create("urn:ngsi-ld:Subscription:stale"));
		TMForumSubscription createdOnPostgres = source.addHub(List.of(PRODUCT), null);

		ExportReport report = run(options(false, false), HUB);

		assertTrue(report.succeeded(), output(report));
		assertEquals(2, report.type(HUB).subscriptionsRegistered);
		assertEquals(2, target.registered.size());
		for (TMForumSubscription hub : List.of(imported, createdOnPostgres)) {
			TMForumSubscription written = target.createdHub(hub.getId());
			Subscription subscription = written.getSubscription();
			assertTrue(target.registered.contains(subscription), "The hub must reference its registered subscription.");
			assertEquals(ExportCommand.exportSubscriptionId(hub.getId()), subscription.getId());
			assertEquals(ENDPOINT, subscription.getNotification().getEndpoint().getUri());
			assertEquals(QUERY, subscription.getQ());
			assertEquals(PRODUCT, subscription.getEntities().getFirst().getType());
		}
		assertNotEquals(URI.create("urn:ngsi-ld:Subscription:stale"), target.createdHub(imported.getId())
				.getSubscription().getId());
	}

	@Test
	void refusesABrokerThatAlreadyNotifiesTheEndpoint() {
		source.addHub(List.of(PRODUCT), null);
		target.subscriptions.add(new SubscriptionStore.BrokerSubscription(URI.create("urn:existing"),
				ENDPOINT.toString()));

		ExportReport report = run(options(false, false), HUB);

		assertFalse(report.succeeded());
		assertTrue(report.type(HUB).error.contains("duplicate"), report.type(HUB).error);
		assertTrue(target.registered.isEmpty());
		assertTrue(target.created.isEmpty());
	}

	@ParameterizedTest(name = "interrupted export: {0}")
	@ValueSource(booleans = {false, true})
	void refusesAHubSubscriptionTheBrokerHoldsUnderAnotherEndpoint(boolean interruptedExport) {
		source.addHub(List.of(PRODUCT), null);
		URI stored = URI.create("urn:ngsi-ld:Subscription:held");
		TMForumSubscription hub = source.addHub(List.of(PRODUCT), interruptedExport ? null : stored);
		URI held = interruptedExport ? ExportCommand.exportSubscriptionId(hub.getId()) : stored;
		target.subscriptions.add(new SubscriptionStore.BrokerSubscription(held, OTHER_ENDPOINT_SPELLING));

		ExportReport report = run(options(false, false), HUB);

		assertFalse(report.succeeded());
		String error = report.type(HUB).error;
		assertTrue(error.contains(held.toString()) && error.contains("--keep-broker-subscriptions"), error);
		assertTrue(target.registered.isEmpty(), "No hub may get a subscription before the check passed.");
		assertTrue(target.created.isEmpty());
	}

	@Test
	void keepsTheSubscriptionsTheBrokerHoldsOnRequest() {
		URI held = URI.create("urn:ngsi-ld:Subscription:held");
		TMForumSubscription withHeld = source.addHub(List.of(PRODUCT), held);
		TMForumSubscription fresh = source.addHub(List.of(PRODUCT), null);
		target.subscriptions.add(new SubscriptionStore.BrokerSubscription(held, OTHER_ENDPOINT_SPELLING));
		target.subscriptions.add(new SubscriptionStore.BrokerSubscription(URI.create("urn:orphan"),
				ENDPOINT.toString()));

		TypeReport report = run(options(false, true, false), HUB).type(HUB);

		assertTrue(report.succeeded(), String.valueOf(report.failures));
		assertEquals(1, report.subscriptionsKept);
		assertEquals(1, report.subscriptionsRegistered);
		assertEquals(2, report.written);
		assertEquals(held, target.createdHub(withHeld.getId()).getSubscription().getId());
		assertTrue(target.registered.contains(target.createdHub(fresh.getId()).getSubscription()));
	}

	@Test
	void aResumeKeepsSubscriptionsTheBrokerHoldsAndSkipsExportedHubs() {
		URI kept = URI.create("urn:ngsi-ld:Subscription:kept");
		TMForumSubscription withKept = source.addHub(List.of(PRODUCT), kept);
		TMForumSubscription exported = source.addHub(List.of(PRODUCT), null);
		TMForumSubscription fresh = source.addHub(List.of(PRODUCT), URI.create("urn:ngsi-ld:Subscription:gone"));
		target.subscriptions.add(new SubscriptionStore.BrokerSubscription(kept, ENDPOINT.toString()));
		URI exportedSubscription = ExportCommand.exportSubscriptionId(exported.getId());
		target.storeHub(exported.getId(), exportedSubscription);
		target.subscriptions.add(new SubscriptionStore.BrokerSubscription(exportedSubscription, ENDPOINT.toString()));

		TypeReport report = run(options(true, false), HUB).type(HUB);

		assertTrue(report.succeeded(), String.valueOf(report.failures));
		assertEquals(1, report.subscriptionsKept);
		assertEquals(1, report.subscriptionsRegistered);
		assertEquals(1, report.skipped);
		assertEquals(2, report.written);
		assertEquals(kept, target.createdHub(withKept.getId()).getSubscription().getId());
		assertTrue(target.registered.contains(target.createdHub(fresh.getId()).getSubscription()));
	}

	static Stream<Arguments> endpoints() {
		return Stream.of(
				Arguments.of("default", ENDPOINT, Map.of(), List.of(PRODUCT), ENDPOINT),
				Arguments.of("override", ENDPOINT, Map.of(ORDER, ORDER_ENDPOINT), List.of(ORDER), ORDER_ENDPOINT),
				Arguments.of("override without a default", null, Map.of(ORDER, ORDER_ENDPOINT), List.of(ORDER),
						ORDER_ENDPOINT),
				Arguments.of("no entities", ENDPOINT, Map.of(), List.of(), ENDPOINT),
				Arguments.of("types on two deployments", ENDPOINT, Map.of(ORDER, ORDER_ENDPOINT),
						List.of(PRODUCT, ORDER), null),
				Arguments.of("no endpoint", null, Map.of(ORDER, ORDER_ENDPOINT), List.of(PRODUCT), null));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("endpoints")
	void picksTheNotificationEndpointByTheHubsEntities(String description, URI defaultEndpoint,
			Map<String, URI> overrides, List<String> entities, URI expected) {
		ExportOptions options = new ExportOptions(PAGE_SIZE, false, false, false, false, defaultEndpoint, overrides);

		if (expected == null) {
			assertThrows(IllegalArgumentException.class, () -> options.notificationEndpoint(entities), description);
		} else {
			assertEquals(expected, options.notificationEndpoint(entities), description);
		}
	}

	@Test
	void aHubWithoutAnEndpointFailsAndRegistersNothing() {
		source.addHub(List.of(PRODUCT), null);

		ExportOptions withoutEndpoint = new ExportOptions(PAGE_SIZE, false, false, false, false, null, Map.of());

		TypeReport report = run(withoutEndpoint, HUB).type(HUB);

		assertTrue(report.error.contains("--notification-endpoint"), report.error);
		assertTrue(target.registered.isEmpty());
		assertTrue(target.created.isEmpty());
	}

	private enum HubCheckFailure {
		SUBSCRIPTION_NOTIFYING_THE_ENDPOINT("duplicate"),
		HUB_SUBSCRIPTION_UNDER_ANOTHER_ENDPOINT("--keep-broker-subscriptions"),
		NO_NOTIFICATION_ENDPOINT("--notification-endpoint"),
		HUBS_ON_THE_BROKER("empty broker");

		final String expectedError;

		HubCheckFailure(String expectedError) {
			this.expectedError = expectedError;
		}
	}

	@ParameterizedTest
	@EnumSource(HubCheckFailure.class)
	void writesNothingWhenTheHubCheckFails(HubCheckFailure failure) {
		source.addPlain(PAGE_SIZE + 1);
		URI stored = URI.create("urn:ngsi-ld:Subscription:held");
		source.addHub(List.of(PRODUCT), stored);
		URI endpoint = ENDPOINT;
		switch (failure) {
			case SUBSCRIPTION_NOTIFYING_THE_ENDPOINT -> target.subscriptions.add(
					new SubscriptionStore.BrokerSubscription(URI.create("urn:existing"), ENDPOINT.toString()));
			case HUB_SUBSCRIPTION_UNDER_ANOTHER_ENDPOINT -> target.subscriptions.add(
					new SubscriptionStore.BrokerSubscription(stored, OTHER_ENDPOINT_SPELLING));
			case NO_NOTIFICATION_ENDPOINT -> endpoint = null;
			case HUBS_ON_THE_BROKER -> target.counts.put(HUB, 1L);
		}

		ExportReport report = run(new ExportOptions(PAGE_SIZE, false, false, false, false, endpoint, Map.of()), PLAIN,
				HUB);

		assertFalse(report.succeeded());
		String error = report.type(HUB).error;
		assertTrue(error != null && error.contains(failure.expectedError), error);
		assertEquals(0, report.type(PLAIN).read, "No type may be exported before the hub check passed.");
		assertTrue(target.created.isEmpty());
		assertTrue(target.registered.isEmpty());
		assertTrue(output(report).contains("nothing was exported"), output(report));
	}

	@Test
	void aResumeNeedsNoEndpointForHubsTheBrokerHoldsOrKeeps() {
		source.addPlain(1);
		URI kept = URI.create("urn:ngsi-ld:Subscription:kept");
		source.addHub(List.of(PRODUCT), kept);
		target.subscriptions.add(new SubscriptionStore.BrokerSubscription(kept, ENDPOINT.toString()));
		TMForumSubscription exported = source.addHub(List.of(PRODUCT), null);
		URI exportedSubscription = ExportCommand.exportSubscriptionId(exported.getId());
		target.storeHub(exported.getId(), exportedSubscription);
		target.subscriptions.add(new SubscriptionStore.BrokerSubscription(exportedSubscription, ENDPOINT.toString()));

		ExportReport report = run(new ExportOptions(PAGE_SIZE, true, false, false, false, null, Map.of()), PLAIN, HUB);

		assertTrue(report.succeeded(), output(report));
		assertEquals(1, report.type(PLAIN).written);
		assertEquals(1, report.type(HUB).subscriptionsKept);
		assertEquals(1, report.type(HUB).skipped);
	}

	@Test
	void removesTheSubscriptionOfAHubThatWasNotWritten() {
		TMForumSubscription hub = source.addHub(List.of(PRODUCT), null);
		target.failing.add(hub.getId());

		TypeReport report = run(options(false, false), HUB).type(HUB);

		assertEquals(1, report.failed);
		assertEquals(0, report.subscriptionsRegistered);
		assertEquals(target.registered.stream().map(Subscription::getId).toList(), target.deleted);
	}

	@Test
	void keepsTheSubscriptionOfAHubStoredDespiteAFailedWrite() {
		TMForumSubscription hub = source.addHub(List.of(PRODUCT), null);
		target.landingDespiteFailure.add(hub.getId());

		TypeReport failed = run(options(false, false), HUB).type(HUB);

		assertEquals(1, failed.failed);
		assertEquals(1, failed.subscriptionsRegistered);
		assertTrue(target.deleted.isEmpty(), "The stored hub must keep its subscription.");

		target.subscriptions.add(new SubscriptionStore.BrokerSubscription(
				ExportCommand.exportSubscriptionId(hub.getId()), ENDPOINT.toString()));
		TypeReport resumed = run(options(true, false), HUB).type(HUB);

		assertTrue(resumed.succeeded(), String.valueOf(resumed.failures));
		assertEquals(1, resumed.skipped);
		assertEquals(1, target.registered.size(), "The resumed run must not register a second subscription.");
	}

	@Test
	void keepsTheSubscriptionWhenItCannotCheckWhetherTheHubWasStored() {
		TMForumSubscription hub = source.addHub(List.of(PRODUCT), null);
		target.failing.add(hub.getId());
		target.lookupFails = true;

		TypeReport report = run(options(false, false), HUB).type(HUB);

		assertTrue(target.deleted.isEmpty(), "An unchecked hub may have been stored, so its subscription stays.");
		assertTrue(report.failures.stream().anyMatch(failure -> failure.contains("--resume")),
				String.valueOf(report.failures));
	}

	@Test
	void failsAResumedHubTheBrokerHoldsWithoutItsSubscription() {
		TMForumSubscription hub = source.addHub(List.of(PRODUCT), null);
		target.storeHub(hub.getId(), URI.create("urn:ngsi-ld:Subscription:gone"));

		TypeReport report = run(options(true, false), HUB).type(HUB);

		assertEquals(1, report.failed);
		assertEquals(0, report.skipped);
		assertTrue(report.failures.getFirst().contains("never notify"), String.valueOf(report.failures));
		assertTrue(target.registered.isEmpty());
	}

	@Test
	void reusesTheSubscriptionOfAnInterruptedExport() {
		TMForumSubscription hub = source.addHub(List.of(PRODUCT), null);
		URI registered = ExportCommand.exportSubscriptionId(hub.getId());
		target.subscriptions.add(new SubscriptionStore.BrokerSubscription(registered, ENDPOINT.toString()));

		TypeReport report = run(options(true, false), HUB).type(HUB);

		assertTrue(report.succeeded(), String.valueOf(report.failures));
		assertEquals(1, report.subscriptionsKept);
		assertEquals(0, report.subscriptionsRegistered);
		assertTrue(target.registered.isEmpty(), "A second subscription would notify twice.");
		assertEquals(registered, target.createdHub(hub.getId()).getSubscription().getId());
	}

	@Test
	void aDryRunWritesAndRegistersNothing() {
		source.addPlain(1);
		source.addHub(List.of(PRODUCT), null);

		ExportReport report = run(options(false, true), PLAIN, HUB);

		assertTrue(report.succeeded(), output(report));
		assertEquals(1, report.type(PLAIN).written);
		assertEquals(1, report.type(HUB).subscriptionsRegistered);
		assertTrue(target.created.isEmpty());
		assertTrue(target.registered.isEmpty());
		assertTrue(output(report).contains("dry run"));
	}

	@Test
	void exportsHubsAfterEveryOtherType() {
		source.addHub(List.of(PLAIN), null);
		source.addPlain(PAGE_SIZE + 1);

		ExportReport report = run(options(false, false), HUB, PLAIN);

		assertTrue(report.succeeded(), output(report));
		List<Object> created = target.created;
		assertTrue(created.getLast() instanceof TMForumSubscription, "The hub must be written last.");
		assertTrue(created.subList(0, created.size() - 1).stream().noneMatch(TMForumSubscription.class::isInstance),
				"No entity may be written after a hub's subscription is registered.");
	}

	@Test
	void stopsAtTheFirstFailureWithFailFast() {
		source.addPlain(2);
		target.failing.add(((EntityWithId) source.entities.get(PLAIN).getFirst()).getId());

		ExportOptions failFast = new ExportOptions(PAGE_SIZE, false, false, true, false, ENDPOINT, Map.of());

		ExportReport report = run(failFast, PLAIN, HUB);

		assertFalse(report.succeeded());
		assertEquals(1, report.type(PLAIN).read);
		assertTrue(output(report).contains("--fail-fast"));
	}

	private ExportReport run(ExportOptions options, String... types) {
		return new ExportCommand(source, target, registry, options).run(List.of(types));
	}

	private static ExportOptions options(boolean resume, boolean dryRun) {
		return options(resume, false, dryRun);
	}

	private static ExportOptions options(boolean resume, boolean keepSubscriptions, boolean dryRun) {
		return new ExportOptions(PAGE_SIZE, resume, keepSubscriptions, false, dryRun, ENDPOINT, Map.of());
	}

	private static String output(ExportReport report) {
		StringWriter out = new StringWriter();
		report.print(new PrintWriter(out));
		return out.toString();
	}

	private static final class FakeSource implements ExportSource {
		final Map<String, List<Object>> entities = new HashMap<>();

		void addPlain(int count) {
			for (int i = 0; i < count; i++) {
				entities.computeIfAbsent(PLAIN, type -> new ArrayList<>()).add(new Plain(UUID.randomUUID().toString()));
			}
		}

		TMForumSubscription addHub(List<String> entityTypes, URI storedSubscription) {
			TMForumSubscription hub = new TMForumSubscription(UUID.randomUUID().toString());
			hub.setCallback(CALLBACK);
			hub.setQuery(QUERY);
			hub.setEntities(entityTypes);
			hub.setFields(List.of());
			if (storedSubscription != null) {
				hub.setSubscription(new Subscription(storedSubscription.toString()));
			}
			entities.computeIfAbsent(HUB, type -> new ArrayList<>()).add(hub);
			return hub;
		}

		@Override
		public Page page(String type, Class<?> domainClass, int offset, int limit) {
			List<Object> all = entities.getOrDefault(type, List.of());
			return new Page(List.copyOf(all.subList(Math.min(offset, all.size()), Math.min(offset + limit, all.size()))),
					all.size());
		}
	}

	private static final class FakeTarget implements ExportTarget {
		final Map<String, Long> counts = new HashMap<>();
		final Map<URI, Object> stored = new HashMap<>();
		final Set<URI> failing = new HashSet<>();
		// stored, but the write reports an error, as after a timeout
		final Set<URI> landingDespiteFailure = new HashSet<>();
		final List<Object> created = new ArrayList<>();
		final List<Subscription> registered = new ArrayList<>();
		final List<SubscriptionStore.BrokerSubscription> subscriptions = new ArrayList<>();
		final List<URI> deleted = new ArrayList<>();
		boolean lookupFails;

		void store(Object entity) {
			stored.put(((EntityWithId) entity).getId(), entity);
		}

		void storeHub(URI id, URI subscriptionId) {
			TMForumSubscription hub = new TMForumSubscription(id.toString());
			hub.setSubscription(new Subscription(subscriptionId.toString()));
			store(hub);
		}

		TMForumSubscription createdHub(URI id) {
			return created.stream().map(TMForumSubscription.class::cast).filter(hub -> hub.getId().equals(id))
					.findFirst().orElseThrow();
		}

		@Override
		public long count(String type) {
			return counts.getOrDefault(type, 0L);
		}

		@Override
		public Optional<TMForumSubscription> hub(URI id) {
			if (lookupFails) {
				throw new IllegalStateException("broker down");
			}
			return Optional.ofNullable(stored.get(id)).map(TMForumSubscription.class::cast);
		}

		@Override
		public void create(Object domainEntity) {
			URI id = ((EntityWithId) domainEntity).getId();
			if (stored.containsKey(id)) {
				throw new StorageException(StorageOperation.CREATE, StorageErrorReason.CONFLICT, "taken");
			}
			if (failing.contains(id)) {
				throw new IllegalStateException("broker down");
			}
			store(domainEntity);
			if (landingDespiteFailure.contains(id)) {
				throw new IllegalStateException("timed out");
			}
			created.add(domainEntity);
		}

		@Override
		public void registerSubscription(Subscription subscription) {
			registered.add(subscription);
		}

		@Override
		public List<BrokerSubscription> subscriptions(int pageSize) {
			return List.copyOf(subscriptions);
		}

		@Override
		public boolean deleteSubscription(URI subscriptionId) {
			deleted.add(subscriptionId);
			return true;
		}
	}
}
