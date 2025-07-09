package org.fiware.tmforum.product;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.service.CharacteristicValueSpecification;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProductSpecificationCharacteristicValueUse extends Entity {

	private String tmfId;
	private String description;
	private Integer maxCardinality;
	private Integer minCardinality;
	private String name;
	private String valueType;
	private List<CharacteristicValueSpecification> productSpecCharacteristicValue;
	private ProductSpecificationRef productSpecification;
	private TimePeriod validFor;
}
