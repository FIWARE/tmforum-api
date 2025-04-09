package org.fiware.tmforum.account;

import lombok.RequiredArgsConstructor;
import org.fiware.account.model.*;
import org.fiware.tmforum.account.domain.*;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.notification.EventMapper;

import javax.inject.Singleton;
import java.util.Map;

import static java.util.Map.entry;

@RequiredArgsConstructor
@Singleton
public class AccountEventMapper implements EventMapper {

	private final TMForumMapper tmForumMapper;

	@Override
	public Map<String, EventMapping> getEntityClassMapping() {
		return Map.ofEntries(
				entry(BillFormat.TYPE_BILLF, new EventMapping(BillFormatVO.class, BillFormat.class)),
				entry(BillingAccount.TYPE_BILLINGAC, new EventMapping(BillingAccountVO.class, BillingAccount.class)),
				entry(BillingCycleSpecification.TYPE_BILLCL, new EventMapping(BillingCycleSpecificationVO.class, BillingCycleSpecification.class)),
				entry(BillPresentationMedia.TYPE_BILLPM, new EventMapping(BillPresentationMediaVO.class, BillPresentationMedia.class)),
				entry(FinancialAccount.TYPE_FINANCIALAC, new EventMapping(FinancialAccountVO.class, FinancialAccount.class)),
				entry(PartyAccount.TYPE_PARTYAC, new EventMapping(PartyAccountVO.class, PartyAccount.class)),
				entry(SettlementAccount.TYPE_SETTLEMENTAC, new EventMapping(SettlementAccountVO.class, SettlementAccount.class))
		);
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
