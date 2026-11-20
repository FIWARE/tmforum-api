package org.fiware.tmforum.common.repository;

import io.github.wistefan.mapping.EntityVOMapper;
import io.micronaut.cache.annotation.CacheInvalidate;
import io.micronaut.cache.annotation.Cacheable;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.fiware.ngsi.api.SubscriptionsApiClient;
import org.fiware.ngsi.model.SubscriptionVO;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.exception.DeletionException;
import org.fiware.tmforum.common.exception.DeletionExceptionReason;
import org.fiware.tmforum.common.exception.NgsiLdRepositoryException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Optional;

/**
 * NGSI-LD subscriptions at the broker, the storage of the broker-only notification path. Not wired on PostgreSQL.
 */
@Singleton
@RequiredArgsConstructor
@Requires(notEnv = CommonConstants.POSTGRESQL_ENVIRONMENT)
public class NgsiLdSubscriptionRepository implements SubscriptionRepository {

	private final SubscriptionsApiClient subscriptionsApi;
	private final NgsiLdEntityRepository entityRepository;
	private final EntityVOMapper entityVOMapper;

	/**
	 * Create a subscription at the broker and cache it.
	 *
	 * @param subscriptionVO the subscription to be created
	 * @param ngsiLDTenant   tenant the subscription belongs to
	 * @return completable with the result
	 */
	public Mono<Void> createSubscription(SubscriptionVO subscriptionVO, String ngsiLDTenant) {
		return subscriptionsApi.createSubscription(subscriptionVO, ngsiLDTenant).then();
	}

	@Cacheable(CommonConstants.SUBSCRIPTIONS_CACHE_NAME)
	public Mono<SubscriptionVO> retrieveSubscriptionById(URI subscriptionId) {
		return subscriptionsApi
				.retrieveSubscriptionById(subscriptionId)
				.map(HttpResponse::body)
				.onErrorResume(this::handleClientSubscriptionException);
	}

	/**
	 * Delete a domain subscription
	 *
	 * @param tmForumSubscriptionId id of the tm-forum-subscription to delete the related ngsild-subscription
	 * @return an empty mono
	 */
	public Mono<Void> deleteDomainSubscriptionByTmForumSubscription(URI tmForumSubscriptionId) {
		return entityRepository.retrieveEntityById(tmForumSubscriptionId)
				.flatMap(entityVO -> entityVOMapper.fromEntityVO(entityVO, TMForumSubscription.class))
				.flatMap(tmForumSubscription ->
						deleteDomainSubscription(tmForumSubscription.getSubscription().getId()));
	}

	/**
	 * Delete a domain subscription
	 *
	 * @param subscriptionId id of the ngsild-subscription to be deleted
	 * @return an empty mono
	 */
	@CacheInvalidate(value = CommonConstants.SUBSCRIPTIONS_CACHE_NAME)
	public Mono<Void> deleteDomainSubscription(URI subscriptionId) {
		return subscriptionsApi.removeSubscription(subscriptionId)
				.onErrorResume(t -> {
					if (t instanceof HttpClientResponseException e && e.getStatus().equals(HttpStatus.NOT_FOUND)) {
						throw new DeletionException(String.format("Was not able to delete %s, since it does not exist.",
								subscriptionId), DeletionExceptionReason.NOT_FOUND);
					}
					throw new DeletionException(String.format("Was not able to delete %s.", subscriptionId),
							t, DeletionExceptionReason.UNKNOWN);
				})
				.then();
	}

	private Mono<SubscriptionVO> handleClientSubscriptionException(Throwable e) {
		if (e instanceof HttpClientResponseException httpException && httpException.getStatus().equals(HttpStatus.NOT_FOUND)) {
			return Mono.empty();
		}
		throw new NgsiLdRepositoryException("Was not able to successfully call the broker.", Optional.of(e));
	}
}
