package org.fiware.tmforum.common.notification.checkers.tmforum;

import lombok.RequiredArgsConstructor;
import org.fiware.tmforum.common.querying.SubscriptionQueryResolver;

@RequiredArgsConstructor
public class CreateTmForumEventChecker<T> implements TmForumEventChecker {
    private final SubscriptionQueryResolver subscriptionQueryResolver;
    private final T entity;
    private final String payloadName;

    @Override
    public boolean wasFired(String query) {
        return subscriptionQueryResolver.doesQueryMatchCreateEvent(query, entity, payloadName);
    }
}
