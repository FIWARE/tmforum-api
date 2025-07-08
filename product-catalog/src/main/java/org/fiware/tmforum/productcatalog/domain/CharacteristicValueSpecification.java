package org.fiware.tmforum.productcatalog.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.TimePeriod;

@Data
@EqualsAndHashCode(callSuper = true)
public class CharacteristicValueSpecification extends Entity {

	private Boolean isDefault;
	private String rangeInterval;
	private String regex;
	private String unitOfMeasure;
	private Integer valueFrom;
	private Integer valueTo;
	private TimePeriod validFor;
	private String valueType;
	private Object tmfValue;
}
