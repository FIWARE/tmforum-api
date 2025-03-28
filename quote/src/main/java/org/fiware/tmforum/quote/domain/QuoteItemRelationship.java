package org.fiware.tmforum.quote.domain;

import lombok.Data;
import org.fiware.tmforum.common.domain.Entity;

@Data
public class QuoteItemRelationship extends Entity {

	private String relationshipId;
	private String relationshipType;
}
