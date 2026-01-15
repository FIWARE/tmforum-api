package org.fiware.tmforum.customerbillmanagement;

import lombok.RequiredArgsConstructor;
import org.fiware.customerbillmanagement.model.CustomerBillVO;
import org.fiware.customerbillmanagement.model.CustomerBillOnDemandVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.notification.EventMapper;
import org.fiware.tmforum.customerbillmanagement.domain.CustomerBill;
import org.fiware.tmforum.customerbillmanagement.domain.CustomerBillOnDemand;

import javax.inject.Singleton;
import java.util.Map;

import static java.util.Map.entry;

@RequiredArgsConstructor
@Singleton
public class CustomerBillManagementEventMapper implements EventMapper {

	private final TMForumMapper tmForumMapper;

	@Override
	public Map<String, EventMapping> getEntityClassMapping() {
		return Map.ofEntries(
				entry(CustomerBill.TYPE_CUSTOMER_BILL, new EventMapping(CustomerBillVO.class, CustomerBill.class)),
				entry(CustomerBillOnDemand.TYPE_CUSTOMER_BILL_ON_DEMAND, new EventMapping(CustomerBillOnDemandVO.class, CustomerBillOnDemand.class))
		);
	}

	@Override
	public Object mapPayload(Object rawPayload, Class<?> rawClass) {
		if (rawClass == CustomerBill.class) {
			return tmForumMapper.map((CustomerBill) rawPayload);
		}
		if (rawClass == CustomerBillOnDemand.class) {
			return tmForumMapper.map((CustomerBillOnDemand) rawPayload);
		}
		throw new TmForumException(String.format("Event-Payload %s is not supported.", rawPayload), TmForumExceptionReason.INVALID_DATA);
	}
}
