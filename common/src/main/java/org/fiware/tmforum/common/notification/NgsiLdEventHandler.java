package org.fiware.tmforum.common.notification;

import com.google.common.base.CaseFormat;
import io.github.wistefan.mapping.EntityVOMapper;
import io.micronaut.context.annotation.Bean;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.NotificationVO;
import org.fiware.ngsi.model.SubscriptionVO;
import org.fiware.tmforum.common.notification.checkers.ngsild.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Bean
public class NgsiLdEventHandler extends EventHandler {
    private static final List<NgsiLdEventChecker> NGSI_LD_EVENT_CHECKERS = List.of(
        new CreateNgsiLdEventChecker(),
        new AttributeValueChangeNgsiLdEventChecker(),
        new StateChangeNgsiLdEventChecker(),
        new DeleteNgsiLdEventChecker()
    );

    public NgsiLdEventHandler(NotificationSender notificationSender, EntityVOMapper entityVOMapper) {
        super(notificationSender, entityVOMapper);
    }

    public Mono<Void> handle(NotificationVO notificationVO, SubscriptionVO subscriptionVO,
                             Map<String, Class<?>> entityNameToEntityClassMapping) {
        List<Mono<TMForumNotification>> notifications = notificationVO.getData().stream().flatMap(entityVO ->
                NGSI_LD_EVENT_CHECKERS.stream().filter(eventChecker -> eventChecker.wasFired(entityVO))
                        .map(eventChecker -> createNotification(
                                entityVO, eventChecker.eventTypeSuffix(), notificationVO.getNotifiedAt(),
                                subscriptionVO.getWatchedAttributes() == null ? List.of() : new ArrayList<>(
                                        subscriptionVO.getWatchedAttributes()),
                                URI.create(subscriptionVO.getNotification().getEndpoint().getReceiverInfo().get(0).getValue()),
                                entityNameToEntityClassMapping))).toList();

        return Flux.fromIterable(notifications).flatMap(x -> x).collectList()
                .flatMap(notificationSender::sendNotifications);
    }

    private Mono<TMForumNotification> createNotification(EntityVO payload, String eventTypeSuffix, Instant eventTime,
                                                         List<String> selectedFields, URI listenerCallback,
                                                         Map<String, Class<?>> entityNameToEntityClassMapping) {
        if (payload.getType() == null) {
            payload.setType(entityNameToEntityClassMapping.keySet().iterator().next());
        }
        EventDetails eventDetails = makeEventDetails(payload.getType(), eventTypeSuffix, eventTime);

        return entityVOMapper.fromEntityVO(payload, entityNameToEntityClassMapping.get(eventDetails.entityType()))
                .map(entity -> new TMForumNotification(listenerCallback, createEvent(eventDetails, applyFieldsFilter(
                        entity, selectedFields))));
    }

    private EventDetails makeEventDetails(String entityType, String eventTypeSuffix, Instant eventTime) {
        String payloadName = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, entityType);
        return new EventDetails(entityType,
                CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, entityType) + eventTypeSuffix,
                eventTime, payloadName);
    }

}
