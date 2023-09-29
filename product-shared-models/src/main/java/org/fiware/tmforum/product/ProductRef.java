package org.fiware.tmforum.product;

import io.github.wistefan.mapping.annotations.MappingEnabled;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.resource.Resource;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = Product.TYPE_PRODUCT)
public class ProductRef extends RefEntity {

	public ProductRef(String id) {
		super(id);
	}

	@Override
	public List<String> getReferencedTypes() {
		return List.of(Product.TYPE_PRODUCT);
	}
}
