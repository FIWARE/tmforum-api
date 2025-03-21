package org.fiware.tmforum.common.querying;

import lombok.Data;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class SubscriptionQuery {
	private List<String> eventTypes = new ArrayList<>();
	private List<String> fields = new ArrayList<>();
	private Set<String> eventGroups = new HashSet<>();
	private String query;


	public void addEventType(String eventType) {
		eventTypes.add(eventType);
	}

	public SubscriptionQuery eventTypes(List<String> eventTypes) {
		this.eventTypes = eventTypes;
		return this;
	}

	public SubscriptionQuery eventGroups(Set<String> eventGroups) {
		this.eventGroups = eventGroups;
		return this;
	}

	public SubscriptionQuery query(String query) {
		this.query = query;
		return this;
	}

	public SubscriptionQuery fields(List<String> fields) {
		this.fields = fields;
		return this;
	}
}
