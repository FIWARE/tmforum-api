package org.fiware.tmforum.product;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.Quantity;
import org.fiware.tmforum.common.domain.TimePeriod;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProductTerm extends Entity {

	private String description;
	private String name;
	// thats really true according to the spec
	private Quantity duration;
	private TimePeriod validFor;
}
