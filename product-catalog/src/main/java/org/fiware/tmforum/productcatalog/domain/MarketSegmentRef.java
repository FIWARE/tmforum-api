package org.fiware.tmforum.productcatalog.domain;

import lombok.EqualsAndHashCode;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled
public class MarketSegmentRef extends RefEntity {

	public MarketSegmentRef(String id) {
		super(id);
	}

	@Override
	public List<String> getReferencedTypes() {
		return List.of("market-segment");
	}
}
