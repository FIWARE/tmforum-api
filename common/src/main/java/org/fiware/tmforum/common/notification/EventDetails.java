package org.fiware.tmforum.common.notification;

import java.time.Instant;

public record EventDetails(String entityType, String eventType, Instant eventTime, String payloadName) {
}
