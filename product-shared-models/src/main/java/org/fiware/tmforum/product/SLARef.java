package org.fiware.tmforum.product;

import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.util.List;

@MappingEnabled
@EqualsAndHashCode(callSuper = true)
public class SLARef extends RefEntity {

	public SLARef(String id) {
		super(id);
	}

	@Override
	public List<String> getReferencedTypes() {
		return List.of("sla");
	}
}
