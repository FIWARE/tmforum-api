package org.fiware.tmforum.customerbillmanagement.domain;

import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@EqualsAndHashCode(callSuper = true)
public class ProductRef extends RefEntity {

	public ProductRef(String id) {
		super(id);
	}

	@Override public List<String> getReferencedTypes() {
		return Optional.ofNullable(getAtReferredType()).map(List::of).orElse(List.of());
	}
}
