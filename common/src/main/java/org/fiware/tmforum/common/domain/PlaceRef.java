package org.fiware.tmforum.common.domain;

import lombok.EqualsAndHashCode;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.util.List;

// TODO: Concrete implementations are GeoAddress, Site and Location. Need to be fullfilled in the future.
@EqualsAndHashCode(callSuper = true)
@MappingEnabled
public class PlaceRef extends RefEntity {

	public PlaceRef(String id) {
		super(id);
	}

	@Override
	public List<String> getReferencedTypes() {
		return List.of("place");
	}

}
