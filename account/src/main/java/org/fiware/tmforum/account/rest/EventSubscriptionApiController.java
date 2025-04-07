package org.fiware.tmforum.account.rest;

import io.github.wistefan.mapping.EntityVOMapper;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.account.api.EventsSubscriptionApi;
import org.fiware.account.model.*;
import org.fiware.tmforum.account.TMForumMapper;
import org.fiware.tmforum.account.domain.*;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.mapping.SubscriptionMapper;
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
	private static final Map<String, EventMapping> ENTITY_NAME_TO_ENTITY_CLASS_MAPPING = Map.ofEntries(
			entry(BillFormat.TYPE_BILLF, new EventMapping(BillFormatVO.class, BillFormat.class)),
			entry(BillingAccount.TYPE_BILLINGAC, new EventMapping(BillingAccountVO.class, BillingAccount.class)),
			entry(BillingCycleSpecification.TYPE_BILLCL, new EventMapping(BillingCycleSpecificationVO.class, BillingCycleSpecification.class)),
			entry(BillPresentationMedia.TYPE_BILLPM, new EventMapping(BillPresentationMediaVO.class, BillPresentationMedia.class)),
			entry(FinancialAccount.TYPE_FINANCIALAC, new EventMapping(FinancialAccountVO.class, FinancialAccount.class)),
			entry(PartyAccount.TYPE_PARTYAC, new EventMapping(PartyAccountVO.class, PartyAccount.class)),
			entry(SettlementAccount.TYPE_SETTLEMENTAC, new EventMapping(SettlementAccountVO.class, SettlementAccount.class))
	);

	public EventSubscriptionApiController(QueryParser queryParser, ReferenceValidationService validationService,
										  TmForumRepository repository, TMForumMapper tmForumMapper,
										  TMForumEventHandler tmForumEventHandler, NgsiLdEventHandler ngsiLdEventHandler,
										  GeneralProperties generalProperties, EntityVOMapper entityVOMapper, SubscriptionMapper subscriptionMapper) {
		super(queryParser, validationService, repository, EVENT_GROUP_TO_ENTITY_NAME_MAPPING,
				ENTITY_NAME_TO_ENTITY_CLASS_MAPPING, tmForumEventHandler, ngsiLdEventHandler,
				generalProperties, entityVOMapper, subscriptionMapper);
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

	@Override
	public Object mapPayload(Object rawPayload, Class<?> targetClass) {
		if (targetClass == BillFormat.class) {
			return tmForumMapper.map((BillFormat) rawPayload);
		}
		if (targetClass == BillingAccount.class) {
			return tmForumMapper.map((BillingAccount) rawPayload);
		}
		if (targetClass == BillingCycleSpecification.class) {
			return tmForumMapper.map((BillingCycleSpecification) rawPayload);
		}
		if (targetClass == BillPresentationMedia.class) {
			return tmForumMapper.map((BillPresentationMedia) rawPayload);
		}
		if (targetClass == FinancialAccount.class) {
			return tmForumMapper.map((FinancialAccount) rawPayload);
		}
		if (targetClass == PartyAccount.class) {
			return tmForumMapper.map((PartyAccount) rawPayload);
		}
		if (targetClass == SettlementAccount.class) {
			return tmForumMapper.map((SettlementAccount) rawPayload);
		}
		throw new TmForumException(String.format("Event-Payload %s is not supported.", rawPayload), TmForumExceptionReason.INVALID_DATA);
	}
}
