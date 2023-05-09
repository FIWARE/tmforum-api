package org.fiware.tmforum.productordering.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.RefEntity;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;

import java.util.List;
import java.util.Optional;

@EqualsAndHashCode(callSuper = true)
public class RelatedChannelRef extends RefEntity {

	public RelatedChannelRef(String id) {
		super(id);
	}

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY, targetName = "role", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "role", fromProperties = true) }))
	private String role;

	@Override
	public List<String> getReferencedTypes() {
		return Optional.ofNullable(getAtReferredType()).map(List::of).orElse(List.of());
	}
}