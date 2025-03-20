package org.fiware.tmforum.product;

import lombok.Data;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.service.CharacteristicValueSpecification;

import java.net.URI;
import java.util.List;

@Data
public class ProductSpecificationCharacteristic {

	private String characteristicId;
	private Boolean configurable;
	private String description;
	private Boolean extensible;
	private Boolean isUnique;
	private Integer maxCardinality;
	private Integer minCardinality;
	private String name;
	private String regex;
	private String valueType;
	private List<ProductSpecificationCharacteristicRelationship> productSpecCharRelationship;
	private List<CharacteristicValueSpecification> productSpecCharacteristicValue;
	private TimePeriod validFor;
	private String atValueSchemaLocation;
	private String atBaseType;
	private URI atSchemaLocation;
	private String atType;
}
