package org.fiware.tmforum.common.notification.command;

import lombok.RequiredArgsConstructor;
import org.fiware.tmforum.common.querying.SubscriptionQueryResolver;

@RequiredArgsConstructor
public class AttributeValueChangeEventCommand<T> implements Command {
    private final SubscriptionQueryResolver subscriptionQueryResolver;
    private final T newState;
    private final T oldState;
    private final String payloadName;

    @Override
    public boolean execute(String query) {
        return subscriptionQueryResolver.doesQueryMatchUpdateEvent(query, newState, oldState, payloadName);
    }
}
