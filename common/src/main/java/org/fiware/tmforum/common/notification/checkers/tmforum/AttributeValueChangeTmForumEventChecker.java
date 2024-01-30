package org.fiware.tmforum.common.notification.checkers.tmforum;

import lombok.RequiredArgsConstructor;
import org.fiware.tmforum.common.querying.SubscriptionQueryResolver;

@RequiredArgsConstructor
public class AttributeValueChangeTmForumEventChecker<T> implements TmForumEventChecker {
    private final SubscriptionQueryResolver subscriptionQueryResolver;
    private final T newState;
    private final T oldState;
    private final String payloadName;

    @Override
    public boolean wasFired(String query) {
        return subscriptionQueryResolver.doesQueryMatchUpdateEvent(query, newState, oldState, payloadName);
    }
}
