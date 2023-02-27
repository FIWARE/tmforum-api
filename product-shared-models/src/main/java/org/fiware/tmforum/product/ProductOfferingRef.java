package org.fiware.tmforum.product;

import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled
public class ProductOfferingRef extends RefEntity {

	public ProductOfferingRef(String id) {
		super(id);
	}

	@Override
	public List<String> getReferencedTypes() {
		return List.of(ProductOffering.TYPE_PRODUCT_OFFERING);
	}
}
