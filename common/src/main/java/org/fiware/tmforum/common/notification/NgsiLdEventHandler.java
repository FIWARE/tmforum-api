package org.fiware.tmforum.common.notification;

import tools.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;
import io.github.wistefan.mapping.EntityVOMapper;
import io.micronaut.context.annotation.Bean;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.NotificationVO;
import org.fiware.ngsi.model.SubscriptionVO;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.notification.checkers.ngsild.*;
import org.fiware.tmforum.common.rest.AbstractSubscriptionApiController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

@Bean
public class NgsiLdEventHandler extends EventHandler {
	private static final List<NgsiLdEventChecker> NGSI_LD_EVENT_CHECKERS = List.of(
			new CreateNgsiLdEventChecker(),
			new AttributeValueChangeNgsiLdEventChecker(),
			new StateChangeNgsiLdEventChecker(),
			new DeleteNgsiLdEventChecker()
	);

	private final TMForumEventHandler tmForumEventHandler;

	public NgsiLdEventHandler(NotificationSender notificationSender, EntityVOMapper entityVOMapper, EventMapper eventMapper,
							  ObjectMapper objectMapper, TMForumEventHandler tmForumEventHandler) {
		super(notificationSender, entityVOMapper, eventMapper, objectMapper);
		this.tmForumEventHandler = tmForumEventHandler;
	}

	public Mono<Void> handle(NotificationVO notificationVO, SubscriptionVO subscriptionVO) {
		List<Mono<TMForumNotification>> notifications = notificationVO.getData().stream().flatMap(entityVO ->
				NGSI_LD_EVENT_CHECKERS.stream().filter(eventChecker -> eventChecker.wasFired(entityVO))
						.map(eventChecker -> createNotification(
								entityVO, eventChecker.eventTypeSuffix(), notificationVO.getNotifiedAt(),
								subscriptionVO.getWatchedAttributes() == null ? List.of() : new ArrayList<>(
										subscriptionVO.getWatchedAttributes()),
								URI.create(subscriptionVO.getNotification().getEndpoint().getReceiverInfo().get(0).getValue()),
								subscriptionVO.getId(), eventMapper.getEntityClassMapping()))).toList();

		return Flux.fromIterable(notifications).flatMap(x -> x).collectList()
				.flatMap(notificationSender::sendNotifications);
	}

	private Mono<TMForumNotification> createNotification(EntityVO payload, String eventTypeSuffix, Instant eventTime,
														 List<String> selectedFields, URI listenerCallback, URI notifiedSubscription,
														 Map<String, EventMapping> entityNameToEntityClassMapping) {
		if (payload.getType() == null) {
			// a module maps several types, so the id names the type better than any key of the mapping
			payload.setType(Optional.ofNullable(payload.getId()).flatMap(IdHelper::typeSegment)
					.filter(entityNameToEntityClassMapping::containsKey)
					.orElseGet(() -> entityNameToEntityClassMapping.keySet().iterator().next()));
		}
		EventDetails eventDetails = makeEventDetails(payload.getType(), eventTypeSuffix, eventTime);
		EventMapping eventMapping = entityNameToEntityClassMapping.get(eventDetails.entityType());
		// createEvent maps the domain object to its VO; mapping it here as well handed the mapper a VO
		Mono<TMForumNotification> notification = entityVOMapper.fromEntityVO(payload, eventMapping.rawClass())
				.map(entity -> createNotification(listenerCallback, eventDetails, entity, selectedFields,
						UUID.randomUUID().toString()));
		if (!EventConstants.DELETE_EVENT_SUFFIX.equals(eventTypeSuffix)) {
			return notification;
		}
		// the broker notifies every subscription of a deletion, whatever the hub's eventType
		return notification.filterWhen(n -> hubTakesEvent(eventDetails, notifiedSubscription));
	}

	// matched by the hub's own NGSI-LD subscription, not its callback, which several hubs may share;
	// getSubscriptions reads only the first page, so a hub beyond it loses the DeleteEvent (until ticket 7)
	private Mono<Boolean> hubTakesEvent(EventDetails eventDetails, URI notifiedSubscription) {
		return tmForumEventHandler.getSubscriptions(eventDetails)
				.map(hubs -> hubs.stream().anyMatch(hub -> hub.getSubscription() != null
						&& notifiedSubscription.equals(hub.getSubscription().getId())));
	}

	private EventDetails makeEventDetails(String entityType, String eventTypeSuffix, Instant eventTime) {
		String payloadName = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, entityType);
		return new EventDetails(entityType,
				CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, entityType) + eventTypeSuffix,
				eventTime, payloadName);
	}

}
