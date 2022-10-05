package org.fiware.tmforum.productcatalog.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.party.domain.TimePeriod;


@Data
@EqualsAndHashCode(callSuper = true)
public class ProductOfferingTerm extends Entity {

	private String description;
	private String name;
	private Duration duration;
	private TimePeriod validFor;

}
