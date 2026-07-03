package org.fiware.tmforum.common.repository;

import io.github.wistefan.mapping.EntityVOMapper;
import io.github.wistefan.mapping.JavaObjectMapper;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import io.micronaut.cache.annotation.CacheInvalidate;
import io.micronaut.cache.annotation.CachePut;
import io.micronaut.cache.annotation.Cacheable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import lombok.RequiredArgsConstructor;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.api.SubscriptionsApiClient;
import org.fiware.ngsi.model.EntityFragmentVO;
import org.fiware.ngsi.model.EntityListVO;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.SubscriptionVO;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.caching.EntityIdKeyGenerator;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.exception.DeletionException;
import org.fiware.tmforum.common.exception.DeletionExceptionReason;
import org.fiware.tmforum.common.exception.NgsiLdRepositoryException;
import org.fiware.tmforum.common.mapping.NGSIMapper;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Base-Repository implementation for using the NGSI-LD API as a storage backend. Supports caching and asynchronous
 * retrieval of entities and subscriptions.
 */
@RequiredArgsConstructor
public abstract class NgsiLdBaseRepository {

	protected final GeneralProperties generalProperties;
	protected final EntitiesApiClient entitiesApi;
	protected final SubscriptionsApiClient subscriptionsApi;
	protected final JavaObjectMapper javaObjectMapper;
	protected final NGSIMapper ngsiMapper;
	protected final EntityVOMapper entityVOMapper;


	protected String getLinkHeader() {
		return String.format("<%s>; rel=\"http://www.w3.org/ns/json-ld#context\"; type=\"application/ld+json", generalProperties.getContextUrl());
	}

	/**
	 * Create an entity at the broker and cache it.
	 *
	 * @param entityVO     - the entity to be created
	 * @param ngsiLDTenant - tenant the entity belongs to
	 * @return completable with the result
	 */
	@CachePut(value = CommonConstants.ENTITIES_CACHE_NAME, keyGenerator = EntityIdKeyGenerator.class)
	public Mono<Void> createEntity(EntityVO entityVO, String ngsiLDTenant) {
		return entitiesApi.createEntity(entityVO, ngsiLDTenant).then();
	}

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

	/**
	 * Retrieve entity from the broker or from the cache if they are available there.
	 *
	 * @param entityId id of the entity
	 * @return the entity
	 */
	@Cacheable(CommonConstants.ENTITIES_CACHE_NAME)
	public Mono<EntityVO> retrieveEntityById(URI entityId) {
		return asyncRetrieveEntityById(entityId, generalProperties.getTenant(), null, null, null, getLinkHeader());
	}

	@Cacheable(CommonConstants.SUBSCRIPTIONS_CACHE_NAME)
	public Mono<SubscriptionVO> retrieveSubscriptionById(URI subscriptionId) {
		return subscriptionsApi
				.retrieveSubscriptionById(subscriptionId)
				.map(HttpResponse::body)
				.onErrorResume(this::handleClientSubscriptionException);
	}

	/**
	 * Patch an entity, using the "overwrite" option.
	 *
	 * @param entityId         id of the entity
	 * @param entityFragmentVO the entity elements to be updated
	 * @return an empty mono
	 */
	@CacheInvalidate(value = CommonConstants.ENTITIES_CACHE_NAME, keyGenerator = EntityIdKeyGenerator.class)
	public Mono<Void> patchEntity(URI entityId, EntityFragmentVO entityFragmentVO) {
		return entitiesApi.updateEntity(entityId, entityFragmentVO, generalProperties.getTenant(), null).then();
	}

	/**
	 * Replace an entity entirely using upsert with replace semantics. This avoids the array-merge
	 * behaviour introduced in Scorpio 6.0.0 when using POST /entities/{id}/attrs.
	 *
	 * @param entityId id of the entity
	 * @param entityVO the full merged entity to replace with
	 * @return an empty mono
	 */
	@CacheInvalidate(value = CommonConstants.ENTITIES_CACHE_NAME, keyGenerator = EntityIdKeyGenerator.class)
	public Mono<Void> replaceEntity(URI entityId, EntityVO entityVO) {
		EntityListVO entityListVO = new EntityListVO();
		entityListVO.add(entityVO);
		return entitiesApi.batchEntityUpsert(entityListVO, "replace")
				.then()
				.onErrorResume(HttpClientResponseException.class, e -> {
					// Scorpio 6.x may return 204/207 instead of 200; Micronaut treats status
					// mismatch as an error even for 2xx responses. Accept any 2xx as success.
					if (e.getStatus().getCode() >= 200 && e.getStatus().getCode() < 300) {
						return Mono.empty();
					}
					return Mono.error(new NgsiLdRepositoryException(
							"Was not able to replace entity via batch upsert.", Optional.of(e)));
				});
	}

	/**
	 * Create a domain entity
	 *
	 * @param domainEntity the entity to be created
	 * @param <T>          the type of the object
	 * @return an empty mono
	 */
	public <T> Mono<Void> createDomainEntity(T domainEntity) {
		return createEntity(javaObjectMapper.toEntityVO(domainEntity), generalProperties.getTenant());
	}

	/**
	 * Create a domain subscription
	 *
	 * @param domainSubscription the subscription to be created
	 * @return an empty mono
	 */
	public Mono<Void> createDomainSubscription(Subscription domainSubscription) {
		if (domainSubscription.getNotification() != null
				&& domainSubscription.getNotification().getAttributes() != null
				&& domainSubscription.getNotification().getAttributes().isEmpty()) {
			// account for the api restriction on non-empty attribute lists.
			domainSubscription.getNotification().setAttributes(null);
		}
		return createSubscription(entityVOMapper.toSubscriptionVO(domainSubscription), generalProperties.getTenant());
	}

	/**
	 * Update a domain entity. Routes to different strategies depending on {@code replaceOnUpdate}:
	 * <ul>
	 *   <li>{@code false} (default, Orion-LD): PATCH /attrs - correctly replaces array attributes.</li>
	 *   <li>{@code true} (Scorpio 6.x): read-merge-write via batchEntityUpsert replace - prevents
	 *       array-append behaviour introduced in Scorpio 6.0.0.</li>
	 * </ul>
	 *
	 * @param id           id of the entity to be updated
	 * @param domainEntity the (possibly partial) domain object carrying the updates
	 * @param <T>          the type of the object
	 * @return an empty mono, or an error if the entity does not exist
	 */
	public <T> Mono<Void> updateDomainEntity(String id, T domainEntity) {
		EntityVO updateEntityVO = javaObjectMapper.toEntityVO(domainEntity);
		URI entityId = URI.create(id);
		if (generalProperties.isReplaceOnUpdate()) {
			return retrieveEntityById(entityId)
					.switchIfEmpty(Mono.error(new NgsiLdRepositoryException(
							String.format("Entity %s does not exist.", id), Optional.empty())))
					.map(existingEntityVO -> mergeForUpdate(existingEntityVO, updateEntityVO))
					.flatMap(mergedEntityVO -> replaceEntity(entityId, mergedEntityVO));
		}
		return patchEntity(URI.create(id), ngsiMapper.map(javaObjectMapper.toEntityVO(domainEntity)));
	}

	private EntityVO mergeForUpdate(EntityVO existing, EntityVO update) {
		update.getAdditionalProperties().forEach(existing::setAdditionalProperties);
		if (update.getLocation() != null) existing.setLocation(update.getLocation());
		if (update.getObservationSpace() != null) existing.setObservationSpace(update.getObservationSpace());
		if (update.getOperationSpace() != null) existing.setOperationSpace(update.getOperationSpace());
		if (update.getModifiedAt() != null) existing.setModifiedAt(update.getModifiedAt());
		if (update.getAtContext() != null) existing.setAtContext(update.getAtContext());
		return existing;
	}

	/**
	 * Delete a domain entity
	 *
	 * @param id id of the entity to be deleted
	 * @return an empty mono
	 */
	@CacheInvalidate(value = CommonConstants.ENTITIES_CACHE_NAME, keyGenerator = EntityIdKeyGenerator.class)
	public Mono<Void> deleteDomainEntity(URI id) {
		return entitiesApi
				.removeEntityById(id, generalProperties.getTenant(), null)
				.onErrorResume(t -> {
					if (t instanceof HttpClientResponseException e && e.getStatus().equals(HttpStatus.NOT_FOUND)) {
						throw new DeletionException(String.format("Was not able to delete %s, since it does not exist.", id),
								DeletionExceptionReason.NOT_FOUND);
					}
					throw new DeletionException(String.format("Was not able to delete %s.", id),
							t,
							DeletionExceptionReason.UNKNOWN);
				})
				.then();
	}

	/**
	 * Delete a domain subscription
	 *
	 * @param tmForumSubscriptionId id of the tm-forum-subscription to delete the related ngsild-subscription
	 * @return an empty mono
	 */
	public Mono<Void> deleteDomainSubscriptionByTmForumSubscription(URI tmForumSubscriptionId) {
		return retrieveEntityById(tmForumSubscriptionId)
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

	/**
	 * Helper method for combining a stream of entities to a single mono.
	 *
	 * @param entityVOStream stream of entities
	 * @param targetClass    target class to map them
	 * @param <T>            type of the target
	 * @return a mono, emitting a list of mapped entities
	 */
	protected <T> Mono<List<T>> zipToList(Stream<EntityVO> entityVOStream, Class<T> targetClass) {
		MappingEnabled mappingAnnotation = targetClass.getAnnotation(MappingEnabled.class);
		Stream<EntityVO> filtered = entityVOStream;
		if (mappingAnnotation != null) {
			List<String> expectedTypes = Arrays.asList(mappingAnnotation.entityType());
			filtered = entityVOStream.filter(entityVO -> expectedTypes.contains(entityVO.getType()));
		}
		return Mono.zip(
				filtered.map(entityVO -> entityVOMapper.fromEntityVO(entityVO, targetClass)).toList(),
				oList -> Arrays.stream(oList).map(targetClass::cast).toList()
		);
	}

	/**
	 * Uncached call to the broker
	 */
	private Mono<EntityVO> asyncRetrieveEntityById(URI entityId, String ngSILDTenant, String attrs, String type, String options, String link) {
		return entitiesApi
				.retrieveEntityById(entityId, ngSILDTenant, attrs, type, options, link)
				.map(HttpResponse::body)
				.onErrorResume(this::handleClientEntityException);
	}

	private Mono<EntityVO> handleClientEntityException(Throwable e) {
		if (e instanceof HttpClientResponseException httpException && httpException.getStatus().equals(HttpStatus.NOT_FOUND)) {
			return Mono.empty();
		}
		throw new NgsiLdRepositoryException("Was not able to successfully call the broker.", Optional.of(e));
	}

	private Mono<SubscriptionVO> handleClientSubscriptionException(Throwable e) {
		if (e instanceof HttpClientResponseException httpException && httpException.getStatus().equals(HttpStatus.NOT_FOUND)) {
			return Mono.empty();
		}
		throw new NgsiLdRepositoryException("Was not able to successfully call the broker.", Optional.of(e));
	}

}

