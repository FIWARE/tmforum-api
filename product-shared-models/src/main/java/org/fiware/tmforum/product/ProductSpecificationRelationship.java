package org.fiware.tmforum.product;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.common.domain.TimePeriod;

import java.util.List;

@MappingEnabled
@EqualsAndHashCode(callSuper = true)
public class ProductSpecificationRelationship extends RefEntity {

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY, targetName = "relationshipType", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "relationshipType", fromProperties = true) }))
	private String relationshipType;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY, targetName = "validFor", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "validFor", fromProperties = true) }))
	private TimePeriod validFor;

	public ProductSpecificationRelationship(String id) {
		super(id);
	}

	@Override
	public List<String> getReferencedTypes() {
		return List.of(ProductSpecification.TYPE_PRODUCT_SPECIFICATION);
	}

}
