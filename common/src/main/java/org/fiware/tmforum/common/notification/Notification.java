package org.fiware.tmforum.common.notification;

import org.fiware.tmforum.common.domain.subscription.Event;

import java.net.URI;

public record Notification(URI callback, Event event) {}
