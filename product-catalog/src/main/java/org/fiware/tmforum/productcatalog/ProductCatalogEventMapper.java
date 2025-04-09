package org.fiware.tmforum.productcatalog;

import lombok.RequiredArgsConstructor;
import org.fiware.productcatalog.model.*;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.notification.EventMapper;
import org.fiware.tmforum.product.Category;
import org.fiware.tmforum.product.ProductOffering;
import org.fiware.tmforum.product.ProductOfferingPrice;
import org.fiware.tmforum.product.ProductSpecification;
import org.fiware.tmforum.productcatalog.domain.Catalog;

import javax.inject.Singleton;
import java.util.Map;

import static java.util.Map.entry;

@RequiredArgsConstructor
@Singleton
public class ProductCatalogEventMapper implements EventMapper {

	private final TMForumMapper tmForumMapper;

	@Override
	public Map<String, EventMapping> getEntityClassMapping() {
		return Map.ofEntries(
				entry(Catalog.TYPE_CATALOG, new EventMapping(CatalogVO.class, Catalog.class)),
				entry(Category.TYPE_CATEGORY, new EventMapping(CategoryVO.class, Category.class)),
				entry(ProductOffering.TYPE_PRODUCT_OFFERING, new EventMapping(ProductOfferingVO.class, ProductOffering.class)),
				entry(ProductOfferingPrice.TYPE_PRODUCT_OFFERING_PRICE, new EventMapping(ProductOfferingPriceVO.class, ProductOfferingPrice.class)),
				entry(ProductSpecification.TYPE_PRODUCT_SPECIFICATION, new EventMapping(ProductSpecificationVO.class, ProductSpecification.class))
		);
	}

	@Override
	public Object mapPayload(Object rawPayload, Class<?> rawClass) {
		if (rawClass == Catalog.class) {
			return tmForumMapper.map((Catalog) rawPayload);
		}
		if (rawClass == Category.class) {
			return tmForumMapper.map((Category) rawPayload);
		}
		if (rawClass == ProductOffering.class) {
			return tmForumMapper.map((ProductOffering) rawPayload);
		}
		if (rawClass == ProductOfferingPrice.class) {
			return tmForumMapper.map((ProductOfferingPrice) rawPayload);
		}
		if (rawClass == ProductSpecification.class) {
			return tmForumMapper.map((ProductSpecification) rawPayload);
		}
		throw new TmForumException(String.format("Event-Payload %s is not supported.", rawPayload), TmForumExceptionReason.INVALID_DATA);
	}
}
