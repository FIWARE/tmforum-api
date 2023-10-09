package org.fiware.tmforum.common.domain;

import lombok.EqualsAndHashCode;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = "agreement")
public class AgreementRef extends RefEntity {

	public AgreementRef(String id) {
		super(id);
	}

	@Override
	public List<String> getReferencedTypes() {
		return List.of("agreement");
	}

}
