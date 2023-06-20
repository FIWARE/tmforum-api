package org.fiware.tmforum.product;

import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;
import io.github.wistefan.mapping.annotations.MappingEnabled;


import java.util.List;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled
public class ChannelRef extends RefEntity {

	public ChannelRef(String id) {
		super(id);
	}

	@Override
	public List<String> getReferencedTypes() {
		return List.of("channel");
	}
}
