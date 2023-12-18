package org.fiware.tmforum.productinventory.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.wistefan.mapping.EntityVOMapper;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.model.NotificationVO;
import org.fiware.productinventory.api.EventsSubscriptionApi;
import org.fiware.productinventory.model.EventSubscriptionInputVO;
import org.fiware.productinventory.model.EventSubscriptionVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.notification.EventConstants;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractSubscriptionApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.product.Product;
import org.fiware.tmforum.productinventory.TMForumMapper;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
import static org.fiware.tmforum.common.notification.EventConstants.EVENT_GROUP_PRODUCT;

@Slf4j
@Controller("${general.basepath:/}")
public class EventSubscriptionApiController extends AbstractSubscriptionApiController implements EventsSubscriptionApi {
	private final TMForumMapper tmForumMapper;
	private static final Map<String, String> EVENT_GROUP_TO_ENTITY_NAME_MAPPING = Map.ofEntries(
		entry(EVENT_GROUP_PRODUCT, Product.TYPE_PRODUCT)
	);
	private static final List<String> EVENT_GROUPS = List.of(EVENT_GROUP_PRODUCT);

	public EventSubscriptionApiController(QueryParser queryParser, ReferenceValidationService validationService,
										  TmForumRepository repository, TMForumMapper tmForumMapper,
										  EventHandler eventHandler, GeneralProperties generalProperties,
										  EntityVOMapper entityVOMapper) {
		super(queryParser, validationService, repository, EVENT_GROUP_TO_ENTITY_NAME_MAPPING, eventHandler,
				generalProperties, entityVOMapper);
		this.tmForumMapper = tmForumMapper;
	}

	@Override
	public Mono<HttpResponse<EventSubscriptionVO>> registerListener(
			@NonNull EventSubscriptionInputVO eventSubscriptionInputVO) {
		TMForumSubscription subscription = buildSubscription(eventSubscriptionInputVO.getCallback(),
				eventSubscriptionInputVO.getQuery(), EVENT_GROUPS);

		return create(subscription)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);
	}

	@Override
	public Mono<HttpResponse<Object>> unregisterListener(@NonNull String id) {
		return delete(id);
	}

	@Post(EventConstants.SUBSCRIPTION_CALLBACK_PATH)
	@Consumes({"application/json;charset=utf-8"})
	public Mono<HttpResponse<Void>> callback(@NonNull @QueryValue String subscriptionId,
											 @NonNull @Body String payload,
											 @Header("Listener-Endpoint") String listenerEndpoint,
											 @Nullable @Header("Selected-Fields") String selectedFields,
											 @Header("Event-Types") String eventTypes) {
		log.debug(String.format("Callback for subscription %s with notification %s", subscriptionId, payload));

		try {
			NotificationVO notificationVO = this.entityVOMapper.readNotificationFromJSON(payload);

			assert !notificationVO.getData().isEmpty();

			eventHandler.handleNgsiLdNotification(notificationVO, eventTypes, listenerEndpoint, selectedFields);
			return Mono.just(HttpResponse.noContent());
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
}
