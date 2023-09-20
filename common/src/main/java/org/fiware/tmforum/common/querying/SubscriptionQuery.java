package org.fiware.tmforum.common.querying;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class SubscriptionQuery {
    private List<String> eventTypes;
    private String query;
    private List<String> fields;
    private Set<String> eventGroups;

    public SubscriptionQuery() {
        eventTypes = new ArrayList<>();
        fields = new ArrayList<>();
        eventGroups = new HashSet<>();
        query = "";
    }

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
