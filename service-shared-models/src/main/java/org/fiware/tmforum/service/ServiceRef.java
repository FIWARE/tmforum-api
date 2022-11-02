package org.fiware.tmforum.service;

import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
public class ServiceRef extends RefEntity {
	public ServiceRef(String id) {
		super(id);
	}

	@Override public List<String> getReferencedTypes() {
		return List.of("service");
	}
}
