package org.fiware.tmforum.productordering.domain;

import lombok.Data;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.Quantity;

@Data
public class OrderTerm extends Entity {

	private String description;
	private Quantity duration;
}
