package org.fiware.tmforum.productordering.domain;

import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
public class ProductOfferingPriceRef extends RefEntity {

	public ProductOfferingPriceRef(String id) {
		super(id);
	}

	@Override
	public List<String> getReferencedTypes() {
		return List.of(getAtReferredType());
	}
}
