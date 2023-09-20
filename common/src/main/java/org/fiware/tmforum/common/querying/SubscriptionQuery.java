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
        query = "";
    }

    public void addEventType(String eventType) {
        eventTypes.add(eventType);
    }
}
