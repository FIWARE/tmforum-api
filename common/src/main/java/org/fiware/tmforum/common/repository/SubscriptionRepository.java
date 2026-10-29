package org.fiware.tmforum.common.repository;

import org.fiware.ngsi.model.SubscriptionVO;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Broker-only notification path (owned by ticket 1); not part of the storage port, its errors stay broker-specific.
 */
public interface SubscriptionRepository {

	Mono<Void> createSubscription(SubscriptionVO subscriptionVO, String ngsiLDTenant);

	Mono<SubscriptionVO> retrieveSubscriptionById(URI subscriptionId);

	Mono<Void> deleteDomainSubscriptionByTmForumSubscription(URI tmForumSubscriptionId);
}
