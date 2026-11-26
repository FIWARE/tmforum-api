package org.fiware.tmforum.common.notification;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Set;

/**
 * The NGSI-LD types this deployment emits TMForum events for: the keys of its {@link EventMapper}.
 */
@Singleton
public class EventTypes {

	private final Set<String> types;

	@Inject
	public EventTypes(EventMapper eventMapper) {
		this(eventMapper.getEntityClassMapping().keySet());
	}

	public EventTypes(Set<String> types) {
		this.types = Set.copyOf(types);
	}

	public Set<String> types() {
		return types;
	}

	public boolean contains(String type) {
		return types.contains(type);
	}
}
