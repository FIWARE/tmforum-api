package org.fiware.tmforum.productordering.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderItemRelationship extends Entity {

	private String id;
	private String relationshipType;
}
