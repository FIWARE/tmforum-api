package org.fiware.tmforum.common.notification.command;

import lombok.RequiredArgsConstructor;
import org.fiware.tmforum.common.querying.SubscriptionQueryResolver;

@RequiredArgsConstructor
public class CreateEventCommand<T> implements Command {
    private final SubscriptionQueryResolver subscriptionQueryResolver;
    private final T entity;
    private final String payloadName;

    @Override
    public boolean execute(String query) {
        return subscriptionQueryResolver.doesQueryMatchCreateEvent(query, entity, payloadName);
    }
}
