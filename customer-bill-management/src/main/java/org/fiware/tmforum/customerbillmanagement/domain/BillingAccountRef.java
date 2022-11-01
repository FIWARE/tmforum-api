package org.fiware.tmforum.customerbillmanagement.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;

import java.net.URI;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
public class BillingAccountRef extends RefEntity {

	public BillingAccountRef(String id) {
		super(id);
	}

	@Override
	@JsonIgnore
	public List<String> getReferencedTypes() {
		return List.of("billing-account");
	}
}
