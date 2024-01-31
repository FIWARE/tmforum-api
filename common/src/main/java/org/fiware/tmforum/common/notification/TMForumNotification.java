package org.fiware.tmforum.common.notification;

import org.fiware.tmforum.common.domain.subscription.Event;

import java.net.URI;

public record TMForumNotification(URI callback, Event event) {}
