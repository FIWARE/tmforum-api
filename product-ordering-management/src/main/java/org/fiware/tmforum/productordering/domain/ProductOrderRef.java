package org.fiware.tmforum.productordering.domain;

import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
public class ProductOrderRef extends RefEntity {

	public ProductOrderRef(String id) {
		super(id);
	}

	@Override
	public List<String> getReferencedTypes() {
		return List.of(ProductOrder.TYPE_PRODUCT_ORDER);
	}
}
