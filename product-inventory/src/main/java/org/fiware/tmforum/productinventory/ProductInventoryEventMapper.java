package org.fiware.tmforum.productinventory;

import lombok.RequiredArgsConstructor;
import org.fiware.productinventory.model.ProductVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.notification.EventMapper;
import org.fiware.tmforum.product.Product;

import javax.inject.Singleton;
import java.util.Map;

import static java.util.Map.entry;

@RequiredArgsConstructor
@Singleton
public class ProductInventoryEventMapper implements EventMapper {

	private final TMForumMapper tmForumMapper;

	@Override
	public Map<String, EventMapping> getEntityClassMapping() {
		return Map.ofEntries(
				entry(Product.TYPE_PRODUCT, new EventMapping(ProductVO.class, Product.class))
		);
	}

	@Override
	public Object mapPayload(Object rawPayload, Class<?> rawClass) {
		if (rawClass == Product.class) {
			return tmForumMapper.map((Product) rawPayload);
		}
		throw new TmForumException(String.format("Event-Payload %s is not supported.", rawPayload), TmForumExceptionReason.INVALID_DATA);
	}
}
