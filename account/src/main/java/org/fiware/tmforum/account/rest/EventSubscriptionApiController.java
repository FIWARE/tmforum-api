package org.fiware.tmforum.account.rest;

import io.github.wistefan.mapping.EntityVOMapper;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.account.api.EventsSubscriptionApi;
import org.fiware.account.model.EventSubscriptionInputVO;
import org.fiware.account.model.EventSubscriptionVO;
import org.fiware.tmforum.account.TMForumMapper;
import org.fiware.tmforum.account.domain.*;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.notification.NgsiLdEventHandler;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractSubscriptionApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
import static org.fiware.tmforum.common.notification.EventConstants.*;

@Slf4j
@Controller("${general.basepath:/}")
public class EventSubscriptionApiController extends AbstractSubscriptionApiController implements EventsSubscriptionApi {
    private final TMForumMapper tmForumMapper;
    private static final Map<String, String> EVENT_GROUP_TO_ENTITY_NAME_MAPPING = Map.ofEntries(
            entry(EVENT_GROUP_BILL_FORMAT, BillFormat.TYPE_BILLF),
            entry(EVENT_GROUP_BILLING_ACCOUNT, BillingAccount.TYPE_BILLINGAC),
            entry(EVENT_GROUP_BILLING_CYCLE_SPECIFICATION, BillingCycleSpecification.TYPE_BILLCL),
            entry(EVENT_GROUP_BILL_PRESENTATION_MEDIA, BillPresentationMedia.TYPE_BILLPM),
            entry(EVENT_GROUP_FINANCIAL_ACCOUNT, FinancialAccount.TYPE_FINANCIALAC),
            entry(EVENT_GROUP_PARTY_ACCOUNT, PartyAccount.TYPE_PARTYAC),
            entry(EVENT_GROUP_SETTLEMENT_ACCOUNT, SettlementAccount.TYPE_SETTLEMENTAC)
    );
    private static final List<String> EVENT_GROUPS = List.of(EVENT_GROUP_BILL_FORMAT, /*EVENT_GROUP_BILLING_ACCOUNT,*/
    EVENT_GROUP_BILLING_CYCLE_SPECIFICATION, EVENT_GROUP_BILL_PRESENTATION_MEDIA, EVENT_GROUP_FINANCIAL_ACCOUNT,
            EVENT_GROUP_PARTY_ACCOUNT, EVENT_GROUP_SETTLEMENT_ACCOUNT);
    private static final Map<String, Class<?>> ENTITY_NAME_TO_ENTITY_CLASS_MAPPING = Map.ofEntries(
            entry(BillFormat.TYPE_BILLF, BillFormat.class),
            entry(BillingAccount.TYPE_BILLINGAC, BillingAccount.class),
            entry(BillingCycleSpecification.TYPE_BILLCL, BillingCycleSpecification.class),
            entry(BillPresentationMedia.TYPE_BILLPM, BillPresentationMedia.class),
            entry(FinancialAccount.TYPE_FINANCIALAC, FinancialAccount.class),
            entry(PartyAccount.TYPE_PARTYAC, PartyAccount.class),
            entry(SettlementAccount.TYPE_SETTLEMENTAC, SettlementAccount.class)
    );

    public EventSubscriptionApiController(QueryParser queryParser, ReferenceValidationService validationService,
                                          TmForumRepository repository, TMForumMapper tmForumMapper,
                                          TMForumEventHandler tmForumEventHandler, NgsiLdEventHandler ngsiLdEventHandler,
                                          GeneralProperties generalProperties, EntityVOMapper entityVOMapper) {
        super(queryParser, validationService, repository, EVENT_GROUP_TO_ENTITY_NAME_MAPPING,
                ENTITY_NAME_TO_ENTITY_CLASS_MAPPING, tmForumEventHandler, ngsiLdEventHandler,
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
}
