package org.fiware.tmforum.common.notification;

import com.google.common.base.CaseFormat;
import io.github.wistefan.mapping.EntityVOMapper;
import io.micronaut.context.annotation.Bean;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.NotificationVO;
import org.fiware.tmforum.common.exception.EventHandlingException;
import org.fiware.tmforum.common.notification.checkers.ngsild.*;
import org.fiware.tmforum.common.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.fiware.tmforum.common.notification.EventConstants.*;

@Bean
public class NgsiLdEventHandler extends EventHandler {
    public NgsiLdEventHandler(NotificationSender notificationSender, EntityVOMapper entityVOMapper) {
        super(notificationSender, entityVOMapper);
    }

    public Mono<Void> handle(NotificationVO notificationVO, String relevantEventTypes,
                             String listenerCallback, String selectedFields,
                             Map<String, Class<?>> entityNameToEntityClassMapping) {
        List<Mono<TMForumNotification>> notifications = notificationVO.getData().stream().flatMap(entityVO ->
                buildNgsiLdEventCheckers(relevantEventTypes).stream().filter(eventChecker -> eventChecker.wasFired(entityVO))
                        .map(eventChecker -> createNotification(
                                entityVO, eventChecker.eventType(), notificationVO.getNotifiedAt(),
                                selectedFields == null ? List.of() : Arrays.stream(selectedFields.split(",")).toList(),
                                URI.create(listenerCallback), entityNameToEntityClassMapping))).toList();

        return Flux.fromIterable(notifications).flatMap(x -> x).collectList()
                .flatMap(notificationSender::sendNotifications);
    }

    private Mono<TMForumNotification> createNotification(EntityVO payload, String eventType, Instant eventTime,
                                                         List<String> selectedFields, URI listenerCallback,
                                                         Map<String, Class<?>> entityNameToEntityClassMapping) {
        EventDetails eventDetails = makeEventDetails(eventType, eventTime);
        if (payload.getType() == null) {
            payload.setType(eventDetails.entityType());
        }
        return entityVOMapper.fromEntityVO(payload, entityNameToEntityClassMapping.get(eventDetails.entityType()))
                .map(entity -> new TMForumNotification(listenerCallback, createEvent(eventDetails, applyFieldsFilter(
                        entity, selectedFields))));
    }

    private List<NgsiLdEventChecker> buildNgsiLdEventCheckers(String relevantEventTypes) throws EventHandlingException {
        List<NgsiLdEventChecker> ngsiLdEventCheckers = new ArrayList<>();
        Arrays.stream(relevantEventTypes.split(",")).forEach(relevantEventType -> {
            if (relevantEventType.contains(CREATE_EVENT_SUFFIX)) {
                ngsiLdEventCheckers.add(new CreateNgsiLdEventChecker(relevantEventType));
            } else if (relevantEventType.contains(ATTRIBUTE_VALUE_CHANGE_EVENT_SUFFIX)) {
                ngsiLdEventCheckers.add(new AttributeValueChangeNgsiLdEventChecker(relevantEventType));
            } else if (relevantEventType.contains(STATE_CHANGE_EVENT_SUFFIX)) {
                ngsiLdEventCheckers.add(new StateChangeNgsiLdEventChecker(relevantEventType));
            } else if (relevantEventType.contains(DELETE_EVENT_SUFFIX)) {
                ngsiLdEventCheckers.add(new DeleteNgsiLdEventChecker(relevantEventType));
            } else {
                throw new EventHandlingException("Event type was not recognized.");
            }
        });
        return ngsiLdEventCheckers;
    }

    private EventDetails makeEventDetails(String eventType, Instant eventTime) {
        String payloadName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL,
                StringUtils.getEventGroupName(eventType));
        return new EventDetails(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, payloadName), eventType,
                eventTime, payloadName);
    }

}
