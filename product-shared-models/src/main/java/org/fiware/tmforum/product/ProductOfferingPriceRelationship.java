package org.fiware.tmforum.product;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.RefEntity;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.util.List;

@MappingEnabled
@EqualsAndHashCode(callSuper = true)
public class ProductOfferingPriceRelationship extends RefEntity {

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY, targetName = "relationshipType", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "relationshipType", fromProperties = true) }))
	private String relationshipType;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY, targetName = "role", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "role", fromProperties = true) }))
	private String role;

	public ProductOfferingPriceRelationship(String id) {
		super(id);
	}

	@Override
	public List<String> getReferencedTypes() {
		return List.of(ProductOfferingPrice.TYPE_PRODUCT_OFFERING_PRICE);
	}
}
