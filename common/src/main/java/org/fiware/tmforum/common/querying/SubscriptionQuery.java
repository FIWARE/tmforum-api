package org.fiware.tmforum.common.querying;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SubscriptionQuery {
    private List<String> eventTypes;
    private String query;
    private String eventGroupName;
    private List<String> fields;

    public SubscriptionQuery() {
        eventTypes = new ArrayList<>();
        fields = new ArrayList<>();
        query = "";
    }

    public void addEventType(String eventType) {
        eventTypes.add(eventType);
    }

    public SubscriptionQuery eventTypes(List<String> eventTypes) {
        this.eventTypes = eventTypes;
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
