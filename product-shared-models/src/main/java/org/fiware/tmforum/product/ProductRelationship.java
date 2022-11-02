package org.fiware.tmforum.product;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProductRelationship extends Entity {

	private String relationshipType;
	// needs to work as ref or value!
	private ProductRef product;
}
