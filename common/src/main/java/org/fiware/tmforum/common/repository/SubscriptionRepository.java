package org.fiware.tmforum.common.repository;

import org.fiware.ngsi.model.SubscriptionVO;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Broker-only port of the NGSI-LD notification path, absent on PostgreSQL; its errors stay broker-specific.
 */
public interface SubscriptionRepository {

	Mono<Void> createSubscription(SubscriptionVO subscriptionVO, String ngsiLDTenant);

	Mono<SubscriptionVO> retrieveSubscriptionById(URI subscriptionId);

	Mono<Void> deleteDomainSubscriptionByTmForumSubscription(URI tmForumSubscriptionId);
}
