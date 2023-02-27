package org.fiware.tmforum.product;

import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.resource.Resource;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
public class ProductRef extends RefEntity {

	public ProductRef(String id) {
		super(id);
	}

	@Override
	public List<String> getReferencedTypes() {
		return List.of(Product.TYPE_PRODUCT);
	}
}
