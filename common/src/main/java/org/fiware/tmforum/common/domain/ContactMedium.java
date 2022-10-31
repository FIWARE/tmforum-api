package org.fiware.tmforum.common.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.TimePeriod;

@Data
@EqualsAndHashCode(callSuper = true)
public class ContactMedium extends Entity {

	private String mediumType;
	private boolean preferred;
	private MediumCharacteristic characteristic;
	private TimePeriod validFor;

}
