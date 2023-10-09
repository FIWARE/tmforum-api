package org.fiware.tmforum.common.domain.subscription;

import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
public class Event {
    private String eventId;
    private Instant eventTime;
    private String eventType;
    private String correlationId;
    private String domain;
    private String title;
    private String description;
    private String priority;
    private Instant timeOcurred;
    private String fieldPath;
    private Map<String, Object> event;
}
