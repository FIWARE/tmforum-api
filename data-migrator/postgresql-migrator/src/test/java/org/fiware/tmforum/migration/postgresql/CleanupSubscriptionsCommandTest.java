package org.fiware.tmforum.migration.postgresql;

import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.migration.postgresql.CleanupSubscriptionsReport.Kind;
import org.fiware.tmforum.migration.postgresql.SubscriptionStore.BrokerSubscription;
import org.fiware.tmforum.storage.StorageOperation;
import org.fiware.tmforum.storage.postgresql.DocumentMode;
import org.fiware.tmforum.storage.postgresql.PostgresDocumentMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CleanupSubscriptionsCommandTest {

	private static final String CALLBACK_ENDPOINT = "http://tmforum:8080/callback";
	private static final String FOREIGN_ENDPOINT = "http://other:8080/notify";
	private static final URI REFERENCED = URI.create("urn:ngsi-ld:Subscription:referenced");
	private static final URI GONE = URI.create("urn:ngsi-ld:Subscription:gone");
	private static final URI ORPHAN = URI.create("urn:ngsi-ld:Subscription:orphan");
	private static final URI FOREIGN = URI.create("urn:ngsi-ld:Subscription:foreign");
	private static final int PAGE_SIZE = 2;
	private static final StoredHubs.Outcome STORED = new StoredHubs.Outcome(2, 2);

	private final FakeHubs hubs = new FakeHubs();
	private final FakeSubscriptions subscriptions = new FakeSubscriptions();
	private final FakeStoredHubs storedHubs = new FakeStoredHubs();

	@Test
	void deletesTheReferencedSubscriptionsAndCountsMissingOnesAsDeleted() {
		hubs.add(REFERENCED);
		hubs.add(GONE);
		hubs.add(null);
		subscriptions.stored.put(REFERENCED, CALLBACK_ENDPOINT);

		CleanupSubscriptionsReport report = run(false, false);

		assertTrue(report.succeeded(), printed(report));
		assertEquals(3, report.hubs);
		assertEquals(1, report.hubsWithoutSubscription);
		assertEquals(List.of(REFERENCED, GONE), report.counts(Kind.REFERENCED).found);
		assertEquals(1, report.counts(Kind.REFERENCED).deleted);
		assertEquals(1, report.counts(Kind.REFERENCED).alreadyMissing);
		assertEquals(List.of(REFERENCED, GONE), subscriptions.deleteCalls);
		assertEquals(List.of(false), storedHubs.calls);
		assertEquals(STORED, report.storedHubs());
	}

	@Test
	void deletesASubscriptionSharedByTwoHubsOnce() {
		hubs.add(REFERENCED);
		hubs.add(REFERENCED);
		subscriptions.stored.put(REFERENCED, CALLBACK_ENDPOINT);

		CleanupSubscriptionsReport report = run(false, false);

		assertTrue(report.succeeded(), printed(report));
		assertEquals(List.of(REFERENCED), subscriptions.deleteCalls);
	}

	@Test
	void deletesOnlyOrphansWithACallbackEndpoint() {
		hubs.add(REFERENCED);
		subscriptions.stored.put(REFERENCED, CALLBACK_ENDPOINT);
		subscriptions.stored.put(ORPHAN, CALLBACK_ENDPOINT);
		subscriptions.stored.put(FOREIGN, FOREIGN_ENDPOINT);

		CleanupSubscriptionsReport report = run(false, true);

		assertTrue(report.succeeded(), printed(report));
		assertEquals(List.of(ORPHAN), report.counts(Kind.ORPHAN).found);
		assertEquals(1, report.counts(Kind.ORPHAN).deleted);
		assertEquals(Set.of(FOREIGN), subscriptions.stored.keySet());
		assertEquals(List.of(PAGE_SIZE), subscriptions.listCalls);
	}

	@Test
	void searchesNoOrphansWithoutTheFlag() {
		subscriptions.stored.put(ORPHAN, CALLBACK_ENDPOINT);

		CleanupSubscriptionsReport report = run(false, false);

		assertTrue(report.succeeded(), printed(report));
		assertTrue(subscriptions.listCalls.isEmpty());
		assertTrue(subscriptions.stored.containsKey(ORPHAN));
		assertTrue(printed(report).contains("not searched"), printed(report));
	}

	@Test
	void aDryRunDeletesNothingAndListsWhatItWould() {
		hubs.add(REFERENCED);
		subscriptions.stored.put(REFERENCED, CALLBACK_ENDPOINT);
		subscriptions.stored.put(ORPHAN, CALLBACK_ENDPOINT);

		CleanupSubscriptionsReport report = run(true, true);

		assertTrue(report.succeeded(), printed(report));
		assertTrue(subscriptions.deleteCalls.isEmpty());
		assertEquals(List.of(true), storedHubs.calls);
		String printed = printed(report);
		assertTrue(printed.contains("would delete " + REFERENCED), printed);
		assertTrue(printed.contains("would delete " + ORPHAN), printed);
	}

	@Test
	void reportsAVerifiedNoOpWhenNoStoredHubHoldsASubscription() {
		storedHubs.outcome = new StoredHubs.Outcome(0, 0);

		CleanupSubscriptionsReport report = run(false, false);

		assertTrue(report.succeeded(), printed(report));
		assertTrue(printed(report).contains(CleanupSubscriptionsReport.NO_STORED_SUBSCRIPTION), printed(report));
	}

	static Stream<Arguments> failures() {
		return Stream.of(
				Arguments.of("unmappable hub", (Failure) (h, s, p) -> h.unmappable = true, "mapping hub"),
				Arguments.of("hub paging", (Failure) (h, s, p) -> h.pagingFails = true, "reading the hubs"),
				Arguments.of("subscription listing", (Failure) (h, s, p) -> s.listingFails = true,
						"listing the broker's subscriptions"),
				Arguments.of("deletion", (Failure) (h, s, p) -> s.failing.add(REFERENCED),
						"deleting subscription " + REFERENCED),
				Arguments.of("PostgreSQL", (Failure) (h, s, p) -> p.fails = true, "rolled back"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("failures")
	void recordsAFailureAndStillRunsTheOtherSide(String description, Failure failure, String expected) {
		hubs.add(REFERENCED);
		subscriptions.stored.put(REFERENCED, CALLBACK_ENDPOINT);
		failure.apply(hubs, subscriptions, storedHubs);

		CleanupSubscriptionsReport report = run(false, true);

		assertFalse(report.succeeded(), description);
		assertTrue(report.errors().stream().anyMatch(error -> error.contains(expected)), report.errors().toString());
		assertEquals(List.of(false), storedHubs.calls, description);
		assertTrue(printed(report).contains("Subscription cleanup FAILED."), printed(report));
	}

	@ParameterizedTest(name = "page size {0}")
	@ValueSource(ints = {0, -1})
	void rejectsANonPositivePageSize(int pageSize) {
		assertThrows(IllegalArgumentException.class,
				() -> new CleanupSubscriptionsCommand.Options(pageSize, false, false, Set.of()));
	}

	@Test
	void orphansNeedACallbackEndpoint() {
		assertThrows(IllegalArgumentException.class,
				() -> new CleanupSubscriptionsCommand.Options(PAGE_SIZE, false, true, Set.of()));
	}

	// the reason the PostgreSQL half is no no-op: an imported hub's subscription reaches its stored document
	@Test
	void theStoredDocumentOfAHubHoldsItsSubscription() {
		TMForumSubscription hub = new TMForumSubscription("hub");
		hub.setSubscription(new Subscription(REFERENCED.toString()));

		String document = new PostgresDocumentMapper(JsonMapper.builder().build()).storage()
				.toDocument(hub, DocumentMode.FULL, StorageOperation.CREATE);

		assertTrue(JsonMapper.builder().build().readTree(document).has(RawAttributeCheck.SUBSCRIPTION_ATTRIBUTE),
				document);
	}

	private CleanupSubscriptionsReport run(boolean dryRun, boolean includeOrphans) {
		return new CleanupSubscriptionsCommand(hubs, subscriptions, storedHubs, new CleanupSubscriptionsCommand.Options(
				PAGE_SIZE, dryRun, includeOrphans, Set.of(CALLBACK_ENDPOINT))).run();
	}

	private static String printed(CleanupSubscriptionsReport report) {
		StringWriter out = new StringWriter();
		report.print(new PrintWriter(out));
		return out.toString();
	}

	@FunctionalInterface
	interface Failure {
		void apply(FakeHubs hubs, FakeSubscriptions subscriptions, FakeStoredHubs storedHubs);
	}

	static final class FakeHubs implements EntitySource {
		private final List<EntityVO> entities = new ArrayList<>();
		private final Map<URI, TMForumSubscription> domainEntities = new LinkedHashMap<>();
		boolean unmappable;
		boolean pagingFails;

		void add(URI subscriptionId) {
			TMForumSubscription hub = new TMForumSubscription("hub-" + entities.size());
			if (subscriptionId != null) {
				hub.setSubscription(new Subscription(subscriptionId.toString()));
			}
			entities.add(new EntityVO().id(hub.getId()).type(TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION));
			domainEntities.put(hub.getId(), hub);
		}

		@Override
		public Page page(String type, int offset, int limit) {
			if (pagingFails) {
				throw new IllegalStateException("broker down");
			}
			assertEquals(TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION, type);
			return new Page(entities.subList(Math.min(offset, entities.size()),
					Math.min(offset + limit, entities.size())), entities.size());
		}

		@Override
		public <T> T toDomain(EntityVO entity, Class<T> domainClass) {
			if (unmappable) {
				throw new IllegalStateException("unmappable");
			}
			return domainClass.cast(domainEntities.get(entity.getId()));
		}

		@Override
		public Set<String> types() {
			return Set.of(TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION);
		}
	}

	static final class FakeSubscriptions implements SubscriptionStore {
		private final Map<URI, String> stored = new LinkedHashMap<>();
		private final Set<URI> failing = new HashSet<>();
		private final List<URI> deleteCalls = new ArrayList<>();
		private final List<Integer> listCalls = new ArrayList<>();
		boolean listingFails;

		@Override
		public List<BrokerSubscription> subscriptions(int pageSize) {
			listCalls.add(pageSize);
			if (listingFails) {
				throw new IllegalStateException("listing failed");
			}
			return stored.entrySet().stream()
					.map(entry -> new BrokerSubscription(entry.getKey(), entry.getValue()))
					.toList();
		}

		@Override
		public boolean deleteSubscription(URI subscriptionId) {
			deleteCalls.add(subscriptionId);
			if (failing.contains(subscriptionId)) {
				throw new IllegalStateException("delete failed");
			}
			return stored.remove(subscriptionId) != null;
		}
	}

	static final class FakeStoredHubs implements StoredHubs {
		private final List<Boolean> calls = new ArrayList<>();
		StoredHubs.Outcome outcome = STORED;
		boolean fails;

		@Override
		public Outcome dropSubscriptions(boolean dryRun) {
			calls.add(dryRun);
			if (fails) {
				throw new IllegalStateException("database down");
			}
			return outcome;
		}
	}
}
