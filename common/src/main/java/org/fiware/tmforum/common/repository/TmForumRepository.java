package org.fiware.tmforum.common.repository;

import io.github.wistefan.mapping.EntityVOMapper;
import io.github.wistefan.mapping.JavaObjectMapper;
import io.micronaut.http.HttpResponse;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.NotificationSender;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.NGSIMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.inject.Singleton;
import java.net.URI;
import java.util.List;

import static org.fiware.tmforum.common.CommonConstants.DEFAULT_LIMIT;
import static org.fiware.tmforum.common.CommonConstants.DEFAULT_OFFSET;

@Singleton
public class TmForumRepository extends NgsiLdBaseRepository {
	private final NotificationSender notificationSender;

	public TmForumRepository(GeneralProperties generalProperties, EntitiesApiClient entitiesApi,
							 EntityVOMapper entityVOMapper, NGSIMapper ngsiMapper,
							 JavaObjectMapper javaObjectMapper, NotificationSender notificationSender) {
		super(generalProperties, entitiesApi, javaObjectMapper, ngsiMapper, entityVOMapper);
		this.notificationSender = notificationSender;
	}

	public <T> Mono<T> get(URI id, Class<T> entityClass) {
		return retrieveEntityById(id)
				.flatMap(entityVO -> entityVOMapper.fromEntityVO(entityVO, entityClass));
	}

	public <T> Mono<List<T>> findEntities(Integer offset, Integer limit, String entityType, Class<T> entityClass,
			String query) {
		return entitiesApi.queryEntities(generalProperties.getTenant(),
						null,
						null,
						entityType,
						null,
						query,
						null,
						null,
						null,
						null,
						null,
						limit,
						offset,
						null,
						getLinkHeader())
				.map(List::stream)
				.flatMap(entityVOStream -> zipToList(entityVOStream, entityClass))
				.onErrorResume(t -> {
					throw new TmForumException("Was not able to list entities.", t, TmForumExceptionReason.UNKNOWN);
				});
	}

	private Mono<List<Subscription>> getSubscriptions(String entityType, String eventType) {
		return findEntities(
			DEFAULT_OFFSET,
			DEFAULT_LIMIT,
			Subscription.TYPE_SUBSCRIPTION,
			Subscription.class,
			String.format("entities==\"%s\";eventTypes==\"%s\"", entityType, eventType)
		);
	}

	public <T> Mono<List<HttpResponse<String>>> handleCreateEvent(T entity) {
		String entityType = notificationSender.getEntityType(entity);
		if (entityType.equals(Subscription.TYPE_SUBSCRIPTION)) {
			return Mono.empty();
		}

		String eventType = notificationSender.buildCreateEventType(entityType);
		return getSubscriptions(entityType, eventType)
				.flatMap(subscriptions -> notificationSender.handleCreateEvent(subscriptions, entity));
	}

	public <T> Mono<List<HttpResponse<String>>> handleUpdateEvent(T newState, T oldState) {
		String entityType = notificationSender.getEntityType(newState);
		if (entityType.equals(Subscription.TYPE_SUBSCRIPTION)) {
			return Mono.empty();
		}

		String buildAttributeValueChangeEventType = notificationSender.buildAttributeValueChangeEventType(entityType);
		Flux<HttpResponse<String>> attrUpdateMono = getSubscriptions(entityType, buildAttributeValueChangeEventType)
				.flatMapMany(subscriptions ->
						notificationSender.handleUpdateEvent(subscriptions, oldState, newState));

		String stateChangeEventType = notificationSender.buildStateChangeEventType(entityType);
		Flux<HttpResponse<String>> stateChangeMono = getSubscriptions(entityType, stateChangeEventType)
				.flatMapMany(subscriptions ->
						notificationSender.handleStateChangeEvent(subscriptions, oldState, newState));
		return Flux.merge(attrUpdateMono, stateChangeMono).collectList();
	}

	public Mono<List<HttpResponse<String>>> handleDeleteEvent(EntityVO entityVO) {
		String entityType = entityVO.getType();
		if (entityType.equals(Subscription.TYPE_SUBSCRIPTION)) {
			return Mono.empty();
		}

		String eventType = notificationSender.buildDeleteEventType(entityType);
		return getSubscriptions(entityType, eventType)
				.flatMap(subscriptions -> notificationSender.handleDeleteEvent(subscriptions, entityVO));
	}

}
