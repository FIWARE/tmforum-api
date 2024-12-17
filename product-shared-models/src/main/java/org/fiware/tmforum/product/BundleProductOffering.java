package org.fiware.tmforum.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@MappingEnabled(entityType = ProductOffering.TYPE_PRODUCT_OFFERING)
@EqualsAndHashCode(callSuper = true)
public class BundleProductOffering extends RefEntity implements ReferencedEntity {

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY, targetName = "lifecycleStatus", embedProperty = true)}))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "lifecycleStatus", fromProperties = true)}))
	private String lifecycleStatus;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY, targetName = "bundledProductOfferingOption", embedProperty = true)}))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "bundledProductOfferingOption", fromProperties = true, targetClass = BundleProductOfferingOption.class)}))
	private BundleProductOfferingOption bundledProductOfferingOption;

	public BundleProductOffering(@JsonProperty("id") String id) {
		super(id);
	}

	@Override
	@JsonIgnore
	public List<String> getReferencedTypes() {
		return new ArrayList<>(List.of(ProductOffering.TYPE_PRODUCT_OFFERING));
	}
}
