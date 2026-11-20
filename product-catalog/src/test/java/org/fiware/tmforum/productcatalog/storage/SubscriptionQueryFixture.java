package org.fiware.tmforum.productcatalog.storage;

import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.storage.contract.QueryFixture;
import org.fiware.tmforum.storage.query.AttributePath;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Hubs for the query contract case on a plain string list, {@code entities}, as the hub lookup filters it.
 */
class SubscriptionQueryFixture implements QueryFixture<TMForumSubscription> {

	private static final URI CALLBACK = URI.create("http://hub.example.org/contract-listener");
	private static final String EVENT_TYPE = "ProductOfferingCreateEvent";
	private static final List<String> ENTITY_TYPES = List.of("productOffering");
	private static final AttributePath ENTITIES = new AttributePath(List.of("entities"), true);

	@Override
	public Class<TMForumSubscription> entityClass() {
		return TMForumSubscription.class;
	}

	@Override
	public String storedType() {
		return TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION;
	}

	@Override
	public URI id(TMForumSubscription entity) {
		return entity.getId();
	}

	@Override
	public Set<Attribute> attributes() {
		return Set.of(Attribute.STRING_LIST);
	}

	@Override
	public AttributePath path(Attribute attribute) {
		if (attribute != Attribute.STRING_LIST) {
			throw new IllegalArgumentException("Hubs hold no " + attribute);
		}
		return ENTITIES;
	}

	@Override
	public TMForumSubscription newEntity(URI id, Map<Attribute, Object> values) {
		TMForumSubscription subscription = new TMForumSubscription(id.toString());
		subscription.setCallback(CALLBACK);
		subscription.setRawQuery("");
		subscription.setEventTypes(List.of(EVENT_TYPE));
		subscription.setEntities(((List<?>) values.getOrDefault(Attribute.STRING_LIST, ENTITY_TYPES))
				.stream().map(Object::toString).toList());
		return subscription;
	}
}
