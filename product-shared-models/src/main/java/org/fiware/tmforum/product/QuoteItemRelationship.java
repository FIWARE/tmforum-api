package org.fiware.tmforum.product;

import lombok.Data;
import org.fiware.tmforum.common.domain.Entity;

@Data
public class QuoteItemRelationship extends Entity {

	private String tmfId;
	private String relationshipType;
}
