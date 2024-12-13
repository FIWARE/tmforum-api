package org.fiware.tmforum.customerbillmanagement.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.product.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@EqualsAndHashCode(callSuper = true)
public class ProductRef extends RefEntity {

	public ProductRef(@JsonProperty("id") String id) {
		super(id);
	}

	@Override
	@JsonIgnore
	public List<String> getReferencedTypes() {
		return new ArrayList<>(List.of(Product.TYPE_PRODUCT));
	}
}
