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
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.api.SubscriptionsApiClient;
import org.fiware.ngsi.model.AdditionalPropertyVO;
import org.fiware.ngsi.model.EntityFragmentVO;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.GeoPropertyVO;
import org.fiware.ngsi.model.PropertyTypeVO;
import org.fiware.ngsi.model.PropertyVO;
import org.fiware.ngsi.model.RelationshipVO;
import org.fiware.ngsi.model.SubscriptionVO;
import org.fiware.ngsi.model.UpdateResultVO;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.caching.EntityIdKeyGenerator;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.exception.DeletionException;
import org.fiware.tmforum.common.exception.DeletionExceptionReason;
import org.fiware.tmforum.common.exception.NgsiLdRepositoryException;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.NGSIMapper;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base-Repository implementation for using the NGSI-LD API as a storage backend. Supports caching and asynchronous
 * retrieval of entities and subscriptions.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class NgsiLdBaseRepository {

	protected final GeneralProperties generalProperties;
	protected final EntitiesApiClient entitiesApi;
	protected final SubscriptionsApiClient subscriptionsApi;
	protected final JavaObjectMapper javaObjectMapper;
	protected final NGSIMapper ngsiMapper;
	protected final EntityVOMapper entityVOMapper;

	private static final String DATASET_ID = "datasetId";
	private static final String TYPE = "type";
	private static final URI NGSI_LD_NULL = URI.create("urn:ngsi-ld:null");

	/**
	 * Only needed for updates, therefore injected and not part of the constructor the subclasses have to fulfill.
	 */
	protected EntityAttributesApiClient entityAttributesApi;

	@Inject
	public void setEntityAttributesApi(EntityAttributesApiClient entityAttributesApi) {
		this.entityAttributesApi = entityAttributesApi;
	}


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
	 * Append the given attributes to an entity (POST /attrs), overwriting the instances with the same datasetId.
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
	 * Update a domain entity. Only the attributes the update carries are written, so concurrent updates of different
	 * attributes do not overwrite each other. By default (Orion-LD), they are appended as they are. With
	 * {@code replaceOnUpdate}, each one replaces what is stored for it instead (see
	 * {@link #toAttributeUpdate(Map, EntityVO)}), for brokers that keep the existing instances when appending.
	 *
	 * @param id           id of the entity to be updated
	 * @param domainEntity the (possibly partial) domain object carrying the updates
	 * @param <T>          the type of the object
	 * @return an empty mono, or an error if the entity does not exist
	 */
	public <T> Mono<Void> updateDomainEntity(String id, T domainEntity) {
		EntityVO updateEntityVO = javaObjectMapper.toEntityVO(domainEntity);
		URI entityId = URI.create(id);
		if (!generalProperties.isReplaceOnUpdate()) {
			return patchEntity(entityId, ngsiMapper.map(updateEntityVO));
		}
		return entityAttributesApi
				// read from the broker, a cached entity may miss instances written in the meantime
				.retrieveEntityAsJson(entityId, generalProperties.getTenant(), getLinkHeader())
				.map(HttpResponse::body)
				.onErrorResume(HttpClientResponseException.class, e -> e.getStatus() == HttpStatus.NOT_FOUND
						? Mono.empty()
						: Mono.error(new NgsiLdRepositoryException("Was not able to successfully call the broker.",
								Optional.of(e))))
				.switchIfEmpty(Mono.error(new NgsiLdRepositoryException(
						String.format("Entity %s does not exist.", id), Optional.empty())))
				.map(storedEntity -> toAttributeUpdate(storedEntity, updateEntityVO))
				.flatMap(attributeUpdate -> updateAttributes(entityId, attributeUpdate));
	}

	/**
	 * Write the attributes of an update with Update Attributes (PATCH /attrs). It answers 207 with the attributes
	 * it could not write.
	 *
	 * @param entityId        id of the entity
	 * @param attributeUpdate the attributes to write, see {@link #toAttributeUpdate(Map, EntityVO)}
	 * @return an empty mono on success, an INVALID_DATA error if the broker did not write everything
	 */
	@CacheInvalidate(value = CommonConstants.ENTITIES_CACHE_NAME, keyGenerator = EntityIdKeyGenerator.class)
	public Mono<Void> updateAttributes(URI entityId, EntityFragmentVO attributeUpdate) {
		return entitiesApi.updateEntityAttributes(entityId, attributeUpdate, generalProperties.getTenant())
				.flatMap(response -> failOnPartialUpdate(entityId, response))
				.onErrorResume(HttpClientResponseException.class, e -> {
					// a status that differs from the declared one surfaces as exception, even if it is a 2xx
					if (e.getStatus().getCode() >= 200 && e.getStatus().getCode() < 300) {
						return failOnPartialUpdate(entityId, e.getResponse());
					}
					// propagate as is, so that the controller can map the broker's status
					return Mono.error(e);
				});
	}

	private Mono<Void> failOnPartialUpdate(URI entityId, HttpResponse<?> response) {
		if (response.getStatus() != HttpStatus.MULTI_STATUS) {
			return Mono.empty();
		}
		String details = response.getBody(UpdateResultVO.class)
				.map(UpdateResultVO::getNotUpdated)
				.filter(notUpdated -> !notUpdated.isEmpty())
				.map(notUpdated -> notUpdated.stream()
						.map(attribute -> String.format("%s: %s", attribute.getAttributeName(), attribute.getReason()))
						.collect(Collectors.joining("; ")))
				.orElse("no details given");
		log.warn("Broker did not update all attributes of entity {}: {}", entityId, details);
		return Mono.error(new TmForumException(
				String.format("Not all attributes could be updated: %s", details),
				TmForumExceptionReason.INVALID_DATA));
	}

	/**
	 * Build the fragment that makes each attribute of the update replace the stored one. A TMForum PATCH replaces
	 * a list as a whole, while NGSI-LD only overwrites the instances with the same datasetId and keeps the others.
	 * Therefore every stored instance whose datasetId is not part of the update gets deleted with an NGSI-LD Null,
	 * which also makes an empty list delete the attribute instead of storing an empty array. Attributes that are
	 * not part of the update are not sent, so the broker keeps them as they are.
	 *
	 * @param storedEntity the entity as currently stored, plain json
	 * @param update       the mapped update
	 * @return the attributes to write
	 */
	private EntityFragmentVO toAttributeUpdate(Map<String, Object> storedEntity, EntityVO update) {
		EntityFragmentVO attributeUpdate = ngsiMapper.map(update);
		Map<String, AdditionalPropertyVO> attributes = attributeUpdate.getAdditionalProperties();
		if (attributes == null) {
			return attributeUpdate;
		}
		Map<String, AdditionalPropertyVO> replacements = new LinkedHashMap<>();
		attributes.forEach((name, value) -> replacements.put(name,
				replacingAttribute(value, storedInstances(storedEntity.get(name)))));
		attributes.clear();
		replacements.forEach((name, value) -> {
			// nothing stored and nothing to store - an empty array is not valid NGSI-LD input
			if (value != null) {
				attributeUpdate.setAdditionalProperties(name, value);
			}
		});
		return attributeUpdate;
	}

	private AdditionalPropertyVO replacingAttribute(AdditionalPropertyVO update, List<Map<?, ?>> stored) {
		List<AdditionalPropertyVO> newInstances = attributeInstances(update);
		Set<String> keptDatasetIds = newInstances.stream()
				.map(NgsiLdBaseRepository::datasetId)
				.map(datasetId -> datasetId == null ? null : datasetId.toString())
				.collect(Collectors.toCollection(HashSet::new));
		List<AdditionalPropertyVO> deletions = stored.stream()
				.filter(instance -> !keptDatasetIds.contains(instance.get(DATASET_ID)))
				.map(NgsiLdBaseRepository::deletionOf)
				.toList();
		if (deletions.isEmpty()) {
			return newInstances.isEmpty() ? null : update;
		}
		AttributeInstancesVO instances = new AttributeInstancesVO();
		instances.addAll(newInstances);
		instances.addAll(deletions);
		return instances;
	}

	private static List<AdditionalPropertyVO> attributeInstances(AdditionalPropertyVO attribute) {
		if (attribute instanceof List<?> instances) {
			return instances.stream()
					.filter(AdditionalPropertyVO.class::isInstance)
					.map(AdditionalPropertyVO.class::cast)
					.toList();
		}
		return attribute == null ? List.of() : List.of(attribute);
	}

	private static List<Map<?, ?>> storedInstances(Object storedAttribute) {
		if (storedAttribute instanceof List<?> instances) {
			return instances.stream()
					.filter(Map.class::isInstance)
					.<Map<?, ?>>map(instance -> (Map<?, ?>) instance)
					.toList();
		}
		return storedAttribute instanceof Map<?, ?> instance ? List.of(instance) : List.of();
	}

	private static URI datasetId(AdditionalPropertyVO instance) {
		if (instance instanceof PropertyVO propertyVO) {
			return propertyVO.getDatasetId();
		} else if (instance instanceof RelationshipVO relationshipVO) {
			return relationshipVO.getDatasetId();
		} else if (instance instanceof GeoPropertyVO geoPropertyVO) {
			return geoPropertyVO.getDatasetId();
		}
		return null;
	}

	/**
	 * The NGSI-LD Null deleting the given stored instance. It has to be of the stored type: brokers reject a
	 * Relationship carrying its Null in "value".
	 */
	private static AdditionalPropertyVO deletionOf(Map<?, ?> storedInstance) {
		URI datasetId = Optional.ofNullable(storedInstance.get(DATASET_ID)).map(Object::toString).map(URI::create)
				.orElse(null);
		if (PropertyTypeVO.RELATIONSHIP.getValue().equals(storedInstance.get(TYPE))) {
			return new RelationshipVO()._object(NGSI_LD_NULL).datasetId(datasetId);
		}
		return new PropertyVO().value(NGSI_LD_NULL.toString()).datasetId(datasetId);
	}

	/**
	 * The instances of one attribute, new ones and deletions. They do not share a type if the stored attribute mixes
	 * types, which none of the typed list VOs allows.
	 */
	private static class AttributeInstancesVO extends ArrayList<AdditionalPropertyVO> implements AdditionalPropertyVO {
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
	 * Helper method for combining a stream of entities of potentially different NGSI-LD types to a
	 * single mono, mapping each entity to its own target class instead of one fixed class for the
	 * whole batch. Used for polymorphic list endpoints that query several NGSI-LD types at once
	 * (see {@link TmForumRepository#findEntitiesPolymorphic}).
	 *
	 * @param entityVOStream stream of entities, potentially of different NGSI-LD types
	 * @param typeToClass    resolves the target class to map an entity to, based on its NGSI-LD type
	 * @param <T>            common super-type of all resolved target classes
	 * @return a mono, emitting a list of mapped entities
	 */
	protected <T> Mono<List<T>> zipToPolymorphicList(Stream<EntityVO> entityVOStream,
			Function<String, Class<? extends T>> typeToClass) {
		List<Mono<T>> mappingMonos = entityVOStream
				.map(entityVO -> mapPolymorphicEntity(entityVO, typeToClass))
				.toList();
		if (mappingMonos.isEmpty()) {
			return Mono.just(List.of());
		}
		return Mono.zip(mappingMonos, oList -> Arrays.stream(oList).map(o -> (T) o).toList());
	}

	@SuppressWarnings("unchecked")
	private <T> Mono<T> mapPolymorphicEntity(EntityVO entityVO, Function<String, Class<? extends T>> typeToClass) {
		Class<? extends T> targetClass = typeToClass.apply(entityVO.getType());
		return (Mono<T>) entityVOMapper.fromEntityVO(entityVO, targetClass);
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

