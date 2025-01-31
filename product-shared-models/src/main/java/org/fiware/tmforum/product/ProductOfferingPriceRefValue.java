package org.fiware.tmforum.product;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.common.domain.ReferenceValue;

@MappingEnabled(entityType = {"product-offering-price"})
@EqualsAndHashCode(callSuper = true)
public class ProductOfferingPriceRefValue extends RefEntity {

	public ProductOfferingPriceRefValue(@JsonProperty("id") String id) {
		super(id);
	}

	@Override
	@JsonIgnore
	public List<String> getReferencedTypes() {
		return new ArrayList<>(List.of("product-offering-price"));
	}
}
