package org.fiware.tmforum.product;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.util.List;
import java.util.Optional;

@MappingEnabled(entityType = ProductOffering.TYPE_PRODUCT_OFFERING)
@EqualsAndHashCode(callSuper = true)
public class BundleProductOffering extends RefEntity implements ReferencedEntity {

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY, targetName = "lifecycleStatus", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "lifecycleStatus", fromProperties = true) }))
	private String lifecycleStatus;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY, targetName = "bundledProductOfferingOption", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "bundledProductOfferingOption", fromProperties = true, targetClass = BundleProductOfferingOption.class) }))
	private BundleProductOfferingOption bundledProductOfferingOption;

	public BundleProductOffering(String id) {
		super(id);
	}

	@Override
	public List<String> getReferencedTypes() {
		return Optional.ofNullable(getAtReferredType()).map(List::of).orElse(List.of());
	}
}
