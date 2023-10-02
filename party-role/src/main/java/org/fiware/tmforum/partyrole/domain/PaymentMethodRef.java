package org.fiware.tmforum.partyrole.domain;

import java.util.List;
import java.util.Optional;

import org.fiware.tmforum.common.domain.RefEntity;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class PaymentMethodRef extends RefEntity {

	public PaymentMethodRef(String id) {
		super(id);
	}

	@Override public List<String> getReferencedTypes() {
		return Optional.ofNullable(getAtReferredType()).map(List::of).orElse(List.of());
	}
}
