package org.fiware.tmforum.productordering;

import lombok.RequiredArgsConstructor;
import org.fiware.productordering.model.CancelProductOrderVO;
import org.fiware.productordering.model.ProductOrderVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.notification.EventMapper;
import org.fiware.tmforum.productordering.domain.CancelProductOrder;
import org.fiware.tmforum.productordering.domain.ProductOrder;

import javax.inject.Singleton;
import java.util.Map;

import static java.util.Map.entry;

@RequiredArgsConstructor
@Singleton
public class ProductOrderingEventMapper implements EventMapper {

	private final TMForumMapper tmForumMapper;

	@Override
	public Map<String, EventMapping> getEntityClassMapping() {
		return Map.ofEntries(
				entry(CancelProductOrder.TYPE_CANCEL_PRODUCT_ORDER, new EventMapping(CancelProductOrderVO.class, CancelProductOrder.class)),
				entry(ProductOrder.TYPE_PRODUCT_ORDER, new EventMapping(ProductOrderVO.class, ProductOrder.class))
		);
	}

	@Override
	public Object mapPayload(Object rawPayload, Class<?> rawClass) {
		if (rawClass == CancelProductOrder.class) {
			return tmForumMapper.map((CancelProductOrder) rawPayload);
		}
		if (rawClass == ProductOrder.class) {
			return tmForumMapper.map((ProductOrder) rawPayload);
		}
		throw new TmForumException(String.format("Event-Payload %s is not supported.", rawPayload), TmForumExceptionReason.INVALID_DATA);
	}
}
