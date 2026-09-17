package org.fiware.tmforum.account;

import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.fiware.account.model.BillFormatVO;
import org.fiware.account.model.BillPresentationMediaVO;
import org.fiware.account.model.BillingAccountVO;
import org.fiware.account.model.BillingCycleSpecificationVO;
import org.fiware.account.model.FinancialAccountVO;
import org.fiware.account.model.PartyAccountVO;
import org.fiware.account.model.SettlementAccountVO;
import org.fiware.tmforum.account.domain.BillFormat;
import org.fiware.tmforum.account.domain.BillPresentationMedia;
import org.fiware.tmforum.account.domain.BillingAccount;
import org.fiware.tmforum.account.domain.BillingCycleSpecification;
import org.fiware.tmforum.account.domain.FinancialAccount;
import org.fiware.tmforum.account.domain.PartyAccount;
import org.fiware.tmforum.account.domain.SettlementAccount;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.notification.ModuleEventMapper;

import java.util.Map;

import static java.util.Map.entry;

@RequiredArgsConstructor
@Singleton
public class AccountEventMapper implements ModuleEventMapper {

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
	public Object mapPayload(Object rawPayload, Class<?> rawClass) {
		if (rawClass == BillFormat.class) {
			return tmForumMapper.map((BillFormat) rawPayload);
		}
		if (rawClass == BillingAccount.class) {
			return tmForumMapper.map((BillingAccount) rawPayload);
		}
		if (rawClass == BillingCycleSpecification.class) {
			return tmForumMapper.map((BillingCycleSpecification) rawPayload);
		}
		if (rawClass == BillPresentationMedia.class) {
			return tmForumMapper.map((BillPresentationMedia) rawPayload);
		}
		if (rawClass == FinancialAccount.class) {
			return tmForumMapper.map((FinancialAccount) rawPayload);
		}
		if (rawClass == PartyAccount.class) {
			return tmForumMapper.map((PartyAccount) rawPayload);
		}
		if (rawClass == SettlementAccount.class) {
			return tmForumMapper.map((SettlementAccount) rawPayload);
		}
		throw new TmForumException(String.format("Event-Payload %s is not supported.", rawPayload), TmForumExceptionReason.INVALID_DATA);
	}
}
