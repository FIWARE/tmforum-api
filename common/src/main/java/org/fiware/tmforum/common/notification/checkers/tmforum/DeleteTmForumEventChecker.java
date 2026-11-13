package org.fiware.tmforum.common.notification.checkers.tmforum;

import lombok.RequiredArgsConstructor;
import org.fiware.tmforum.common.querying.SubscriptionQueryResolver;

/**
 * A DeleteEvent goes to a hub whose query the deleted entity matched.
 */
@RequiredArgsConstructor
public class DeleteTmForumEventChecker<T> implements TmForumEventChecker {
    private final SubscriptionQueryResolver subscriptionQueryResolver;
    private final T oldState;
    private final String payloadName;

    @Override
    public boolean wasFired(String query) {
        return subscriptionQueryResolver.doesQueryMatchCreateEvent(query, oldState, payloadName);
    }
}
