package org.fiware.tmforum.productcatalog.domain;

import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
public class BundledProductOfferingPriceRelationship extends RefEntity {

	public BundledProductOfferingPriceRelationship(String id) {
		super(id);
	}

	@Override
	public List<String> getReferencedTypes() {
		return List.of(ProductOfferingPrice.TYPE_PRODUCT_OFFERING_PRICE);
	}
}
