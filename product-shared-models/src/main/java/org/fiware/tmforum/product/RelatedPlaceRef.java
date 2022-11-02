package org.fiware.tmforum.product;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;

import java.util.List;
import java.util.Optional;

@EqualsAndHashCode(callSuper = true)
public class RelatedPlaceRef extends RefEntity {

	public RelatedPlaceRef(String id) {
		super(id);
	}

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "role", embedProperty = true) }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "role", fromProperties = true) }))
	private String role;

	@Override
	public List<String> getReferencedTypes() {
		return Optional.ofNullable(getAtReferredType()).map(List::of).orElse(List.of());
	}
}
