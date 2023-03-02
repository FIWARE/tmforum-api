package org.fiware.tmforum.customer;

import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;

import java.net.URI;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
public class PaymentRef extends RefEntity {

	public PaymentRef(String id) {
		super(id);
	}

	//TODO: check the reference when payment is implemented
	@Override
	public List<String> getReferencedTypes() {
		return List.of("payment");
	}
}
