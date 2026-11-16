package org.fiware.tmforum.productcatalog;

import io.micronaut.context.env.Environment;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.notification.EventDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.product.ProductOffering;
import org.fiware.tmforum.productcatalog.domain.Catalog;
import org.fiware.tmforum.storage.EntityRepository;
import org.fiware.tmforum.storage.contract.KnownDeviation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Looks up the hubs of an event on every backend, PostgreSQL included, while the hub API ITs are still excluded
 * there: the hubs are stored through the repository, so no NGSI-LD subscription is needed. Pins
 * {@link KnownDeviation#MULTI_ELEMENT_LIST_NEVER_MATCHES} on Orion-LD.
 */
@MicronautTest(packages = {"org.fiware.tmforum.productcatalog"}, contextBuilder = FirstMatchConfigContextBuilder.class)
public class HubLookupIT extends AbstractApiIT {

	private static final String CALLBACK = "http://hub.example.org/listener";
	private static final String CREATE_EVENT = "ProductOfferingCreateEvent";
	private static final String DELETE_EVENT = "ProductOfferingDeleteEvent";
	private static final String CATALOG_CREATE_EVENT = "CatalogCreateEvent";
	private static final String CATALOG_DELETE_EVENT = "CatalogDeleteEvent";
	private static final String PAYLOAD_NAME = "productOffering";
	private static final Instant EVENT_TIME = Instant.parse("2026-01-01T00:00:00Z");
	private static final String ORION_LD_ENVIRONMENT = "orion-ld";

	/**
	 * The stored hubs, as the entity types and event types each one takes.
	 */
	enum Hub {
		BOTH_LISTS_MATCH(List.of(Catalog.TYPE_CATALOG, ProductOffering.TYPE_PRODUCT_OFFERING),
				List.of(CATALOG_CREATE_EVENT, CREATE_EVENT, DELETE_EVENT)),
		SINGLE_ELEMENTS_MATCH(List.of(ProductOffering.TYPE_PRODUCT_OFFERING), List.of(CREATE_EVENT)),
		OTHER_ENTITY(List.of(Catalog.TYPE_CATALOG), List.of(CREATE_EVENT, CATALOG_CREATE_EVENT)),
		OTHER_EVENT(List.of(ProductOffering.TYPE_PRODUCT_OFFERING), List.of(DELETE_EVENT));

		private final List<String> entities;
		private final List<String> eventTypes;

		Hub(List<String> entities, List<String> eventTypes) {
			this.entities = entities;
			this.eventTypes = eventTypes;
		}

		private boolean singleElementLists() {
			return entities.size() == 1 && eventTypes.size() == 1;
		}

		private TMForumSubscription subscription() {
			TMForumSubscription subscription = new TMForumSubscription(name());
			subscription.setCallback(URI.create(CALLBACK));
			subscription.setRawQuery("");
			subscription.setEntities(entities);
			subscription.setEventTypes(eventTypes);
			return subscription;
		}
	}

	private final EntityRepository entityRepository;
	private final TMForumEventHandler eventHandler;
	private final Set<KnownDeviation> knownDeviations;

	public HubLookupIT(EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper,
			GeneralProperties generalProperties, EntityRepository entityRepository,
			TMForumEventHandler eventHandler, Environment environment) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.entityRepository = entityRepository;
		this.eventHandler = eventHandler;
		this.knownDeviations = knownDeviations(environment.getActiveNames());
	}

	private static Set<KnownDeviation> knownDeviations(Set<String> activeEnvironments) {
		Set<KnownDeviation> deviations = EnumSet.noneOf(KnownDeviation.class);
		if (!activeEnvironments.contains(CommonConstants.POSTGRESQL_ENVIRONMENT)) {
			deviations.add(KnownDeviation.EMPTY_PAGE_COMPLETES_EMPTY);
		}
		if (activeEnvironments.contains(ORION_LD_ENVIRONMENT)) {
			deviations.add(KnownDeviation.MULTI_ELEMENT_LIST_NEVER_MATCHES);
		}
		return deviations;
	}

	@Override
	protected String getEntityType() {
		return TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION;
	}

	static Stream<Arguments> events() {
		return Stream.of(
				Arguments.of(ProductOffering.TYPE_PRODUCT_OFFERING, CREATE_EVENT,
						Set.of(Hub.BOTH_LISTS_MATCH, Hub.SINGLE_ELEMENTS_MATCH)),
				Arguments.of(ProductOffering.TYPE_PRODUCT_OFFERING, DELETE_EVENT,
						Set.of(Hub.BOTH_LISTS_MATCH, Hub.OTHER_EVENT)),
				Arguments.of(Catalog.TYPE_CATALOG, CATALOG_CREATE_EVENT,
						Set.of(Hub.BOTH_LISTS_MATCH, Hub.OTHER_ENTITY)),
				// hubs take the entity type, but not the event type
				Arguments.of(Catalog.TYPE_CATALOG, CATALOG_DELETE_EVENT, Set.of()));
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("events")
	public void findSubscriptionsReturnsTheHubsTakingTheEvent(String entityType, String eventType,
			Set<Hub> expectedHubs) {
		for (Hub hub : Hub.values()) {
			entityRepository.createDomainEntity(hub.subscription()).block();
		}

		List<TMForumSubscription> page = eventHandler.findSubscriptions(
				new EventDetails(entityType, eventType, EVENT_TIME, PAYLOAD_NAME)).block();
		List<TMForumSubscription> found;
		if (knownDeviations.contains(KnownDeviation.EMPTY_PAGE_COMPLETES_EMPTY)) {
			found = Optional.ofNullable(page).orElse(List.of());
		} else {
			assertNotNull(page, "A page is always emitted, an empty one included.");
			found = page;
		}

		assertEquals(ids(foundToday(expectedHubs)), found.stream().map(TMForumSubscription::getId).collect(Collectors.toSet()),
				"Only hubs whose entities and eventTypes both hold the event's values should be found.");
	}

	private Set<Hub> foundToday(Set<Hub> expectedHubs) {
		if (!knownDeviations.contains(KnownDeviation.MULTI_ELEMENT_LIST_NEVER_MATCHES)) {
			return expectedHubs;
		}
		return expectedHubs.stream().filter(Hub::singleElementLists).collect(Collectors.toSet());
	}

	private static Set<URI> ids(Set<Hub> hubs) {
		return hubs.stream().map(hub -> hub.subscription().getId()).collect(Collectors.toSet());
	}
}
